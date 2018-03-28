package com.infrastructure.loder;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.LinkedHashMap;

import static com.infrastructure.loder.DiskLruCache.readFully;

/**
 * Created by cyc20 on 2018/3/20.
 */

public class Disk implements Cloneable {
    //变量

    static final String JOURNAL_FILE = "journal";      //默认日志名
    static final String JOURNAL_FILE_TMP = "journal.tmp";
    static final String MAGIC = "libcore.io.DiskLruCache";
    static final String VERSION_1 = "1";
    static final long ANY_SEQUENCE_NUMBER = -1;
    private static final String CLEAN = "CLEAN";
    private static final String DIRTY = "DIRTY";
    private static final String REMOVE = "REMOVE";
    private static final String READ = "READ";

    private static final Charset UTF_8 = Charset.forName("UTF-8");  //IO默认UTF-8编码
    private static final int IO_BUFFER_SIZE = 8 * 1024;            //8k的IO缓存

    /**
     * This cache uses a journal file named "journal". A typical journal file
     * looks like this:
     * libcore.io.DiskLruCache
     * 1
     * 100
     * 2
     * <p>
     * CLEAN 3400330d1dfc7f3f7f4b8d4d803dfcf6 832 21054
     * DIRTY 335c4c6028171cfddfbaae1a9c313c52
     * CLEAN 335c4c6028171cfddfbaae1a9c313c52 3934 2342
     * REMOVE 335c4c6028171cfddfbaae1a9c313c52
     * DIRTY 1ab96a171faeeee38496d8b330771a7a
     * CLEAN 1ab96a171faeeee38496d8b330771a7a 1600 234
     * READ 335c4c6028171cfddfbaae1a9c313c52
     * READ 3400330d1dfc7f3f7f4b8d4d803dfcf6
     * <p>
     * The first five lines of the journal form its header. They are the
     * constant string "libcore.io.DiskLruCache", the disk cache's version,
     * the application's version, the value count, and a blank line.
     * <p>
     * Each of the subsequent lines in the file is a record of the state of a
     * cache entry. Each line contains space-separated values: a state, a key,
     * and optional state-specific values.
     * o DIRTY lines track that an entry is actively being created or updated.
     * Every successful DIRTY action should be followed by a CLEAN or REMOVE
     * action. DIRTY lines without a matching CLEAN or REMOVE indicate that
     * temporary files may need to be deleted.
     * o CLEAN lines track a cache entry that has been successfully published
     * and may be read. A publish line is followed by the lengths of each of
     * its values.
     * o READ lines track accesses for LRU.
     * o REMOVE lines track entries that have been deleted.
     * <p>
     * The journal file is appended to as cache operations occur. The journal may
     * occasionally be compacted by dropping redundant lines. A temporary file named
     * "journal.tmp" will be used during compaction; that file should be deleted if
     * it exists when the cache is opened.
     */

    private final File directory;           //数据缓存地址  文件夹地址
    private final File journalFile;         //缓存文件
    private final File journalFileTmp;      //临时文件
    private final int appVersion;           //app版本号
    private final long maxSize;             //最大缓存字节数
    private final int valueCount;           //
    private long size = 0;
    private Writer journalWriter;
    private final LinkedHashMap<String, Disk.Entry> lruEntries
            = new LinkedHashMap<String, Disk.Entry>(0, 0.75f, true);
    private int redundantOpCount;


    private Disk(File directory, int appVersion, int valueCount, long maxSize) {
        this.directory = directory;
        this.appVersion = appVersion;                          //app版本
        this.journalFile = new File(directory, JOURNAL_FILE);
        this.journalFileTmp = new File(directory, JOURNAL_FILE_TMP);
        this.valueCount = valueCount;                      //每个缓存可以分成多少个缓存文件
        this.maxSize = maxSize;
    }

    public static Disk open(File directory,int appVersion,int valueCount,long maxSize){
        //第一步判断valueCount、maxSize的传入参数的合理性

        //第二部进行 cache的初始化

        //第三部进行 目的缓存文件夹是否存在判断
        // 存在，证明已有缓存数据，则对缓存文件进行逐行的读取
        // cache.readJournal(); 调用readJournalLine()函数中 通过每行的内容进行相关操作
        // 之后调用 cache.processJournal() 这个函数作用

        //不存在，则建立缓存文件夹和rebuild缓存文件
        //rebuildJournal()函数中 进行文件首部 和各行具体信息的填充

        return new Disk(directory,appVersion,valueCount,maxSize);
    }

    //rebuildJournal函数
    private synchronized void rebuildJournal() throws IOException {
        if (journalWriter != null) {
            journalWriter.close();
        }

        Writer writer = new BufferedWriter(new FileWriter(journalFileTmp), IO_BUFFER_SIZE);
        writer.write(MAGIC);
        writer.write("\n");
        writer.write(VERSION_1);
        writer.write("\n");
        writer.write(Integer.toString(appVersion));
        writer.write("\n");
        writer.write(Integer.toString(valueCount));
        writer.write("\n");
        writer.write("\n");

        for (Entry entry : lruEntries.values()) {
            if (entry.currentEditor != null) {
                writer.write(DIRTY + ' ' + entry.key + '\n');
            } else {
                writer.write(CLEAN + ' ' + entry.key + entry.getLengths() + '\n');
            }
        }

        writer.close();
        journalFileTmp.renameTo(journalFile);
        journalWriter = new BufferedWriter(new FileWriter(journalFile, true), IO_BUFFER_SIZE);
    }

    //下面说一下 Editor
    /*
    Editor 是定义在DiskLruCache 的内部类  （代理模式）通过这个代理来操作缓存文件
    Entry  是定义在DiskLruCache 另一个的内部类
     */

    private final class Editor{
        private final Entry entry;
        private boolean hasErrors;
        private Editor(Entry entry){
            this.entry=entry;
        }

        //返回一个最后提交的entry的不缓存输入流，如果没有值被提交过返回null
        public InputStream newInputStream(int index) throws IOException {
            synchronized (Disk.this) {
                if (entry.currentEditor != this) {
                    throw new IllegalStateException();
                }
                if (!entry.readable) {
                    return null;
                }
                return new FileInputStream(entry.getCleanFile(index));
            }
        }
        //返回最后提交的entry的文件内容，字符串形式
        public String getString(int index) throws IOException {
            InputStream in = newInputStream(index);
            return in != null ? inputStreamToString(in) : null;
        }
        //返回一个新的无缓冲的输出流，写文件时如果潜在的输出流存在错误，这个edit将被废弃。
        public OutputStream newOutputStream(int index) throws IOException {
            synchronized (Disk.this) {
                if (entry.currentEditor != this) {
                    throw new IllegalStateException();
                }
                return new FaultHidingOutputStream(new FileOutputStream(entry.getDirtyFile(index)));
            }
        }

        //设置entry的value的文件的内容
        public void set(int index, String value) throws IOException {
            Writer writer = null;
            try {
                writer = new OutputStreamWriter(newOutputStream(index), UTF_8);
                writer.write(value);
            } finally {
                closeQuietly(writer);
            }
        }

        /**
         * Commits this edit so it is visible to readers.  This releases the
         * edit lock so another edit may be started on the same key.
         */
        //commit提交编辑的结果，释放edit锁然后其它edit可以启动
        public void commit() throws IOException {
            if (hasErrors) {
                completeEdit(this, false);               //false 删除dirty文件
                remove(entry.key); // the previous entry is stale
            } else {
                completeEdit(this, true);                //将dirty写为clean
            }
        }

        /**
         * Aborts this edit. This releases the edit lock so another edit may be
         * started on the same key.
         */
        //废弃edit，释放edit锁然后其它edit可以启动
        public void abort() throws IOException {
            completeEdit(this, false);
        }

        //包装的输出流类
        private class FaultHidingOutputStream extends FilterOutputStream {
            private FaultHidingOutputStream(OutputStream out) {
                super(out);
            }

            @Override public void write(int oneByte) {
                try {
                    out.write(oneByte);
                } catch (IOException e) {
                    hasErrors = true;
                }
            }

            @Override public void write(byte[] buffer, int offset, int length) {
                try {
                    out.write(buffer, offset, length);
                } catch (IOException e) {
                    hasErrors = true;
                }
            }

            @Override public void close() {
                try {
                    out.close();
                } catch (IOException e) {
                    hasErrors = true;
                }
            }

            @Override public void flush() {
                try {
                    out.flush();
                } catch (IOException e) {
                    hasErrors = true;
                }
            }
        }
    }

    private final class Entry{
        private final String key;

        /**
         * Lengths of this entry's files.
         */
        private final long[] lengths;

        /**
         * True if this entry has ever been published
         */
        private boolean readable;

        /**
         * The ongoing edit or null if this entry is not being edited.
         */
        private Editor currentEditor;

        /**
         * The sequence number of the most recently committed edit to this entry.
         */
        private long sequenceNumber;

        private Entry(String key){
            this.key=key;
            this.lengths=new long[valueCount];
        }

        //获取多个文件长度
        public String getLengths() throws IOException {
            StringBuilder result = new StringBuilder();
            for (long size : lengths) {
                result.append(' ').append(size);
            }
            return result.toString();
        }
        private void setLengths(String[]strings)throws IOException{
            if (strings.length!=valueCount){
                invalidLengths(strings);
            }
            try {
                for (int i = 0; i < strings.length; i++) {
                    lengths[i] = Long.parseLong(strings[i]);
                }
            } catch (NumberFormatException e) {
                throw invalidLengths(strings);
            }
        }
        private IOException invalidLengths(String[] strings) throws IOException {
            throw new IOException("unexpected journal line: " + Arrays.toString(strings));
        }
        public File getCleanFile(int i) {
            return new File(directory, key + "." + i);
        }

        public File getDirtyFile(int i) {
            return new File(directory, key + "." + i + ".tmp");
        }
    }

    //获取Editor

    public Editor edit(String key)throws IOException{
        return edit(key,ANY_SEQUENCE_NUMBER);
    }
    private synchronized Editor edit(String key,long expectedSequenceNumber)throws IOException{
        checkNotClosed();       //检查文件写入操作是否关闭（writer
        validateKey(key);        //检验传入的key是否合法  //外部需要对  URL  进行解析

        Entry entry=lruEntries.get(key);
        if (expectedSequenceNumber != ANY_SEQUENCE_NUMBER
                && (entry == null || entry.sequenceNumber != expectedSequenceNumber)) {
            return null; // snapshot is stale
        }
        if (entry == null) {
            entry = new Disk.Entry(key);
            lruEntries.put(key, entry);
        } else if (entry.currentEditor != null) {
            return null; // another edit is in progress
        }

        Disk.Editor editor = new Disk.Editor(entry);
        entry.currentEditor = editor;

        // flush the journal before creating files to prevent file leaks
        journalWriter.write(DIRTY + ' ' + key + '\n');
        journalWriter.flush();
        return editor;
    }

    private void checkNotClosed() {
        if (journalWriter == null) {
            throw new IllegalStateException("cache is closed");
        }
    }

    private void validateKey(String key){
        if (key.contains(" ") || key.contains("\n") || key.contains("\r")) {
            throw new IllegalArgumentException(
                    "keys must not contain spaces or newlines: \"" + key + "\"");
        }
    }

    //completeEdit  //commit时进行缓存文件的最后操作
    private synchronized void completeEdit(Editor editor,boolean success)throws IOException{
        Entry entry=editor.entry;
        if (entry.currentEditor != editor) {
            throw new IllegalStateException();
        }

        // if this edit is creating the entry for the first time, every index must have a value
        if (success && !entry.readable) {
            for (int i = 0; i < valueCount; i++) {
                if (!entry.getDirtyFile(i).exists()) {
                    editor.abort();
                    throw new IllegalStateException("edit didn't create file " + i);
                }
            }
        }
        //将dirty写为clean
        for (int i = 0; i < valueCount; i++) {
            File dirty = entry.getDirtyFile(i);
            if (success) {
                if (dirty.exists()) {
                    File clean = entry.getCleanFile(i);
                    dirty.renameTo(clean);
                    long oldLength = entry.lengths[i];
                    long newLength = clean.length();
                    entry.lengths[i] = newLength;
                    size = size - oldLength + newLength;
                }
            } else {
                deleteIfExists(dirty);
            }
        }

        redundantOpCount++;
        entry.currentEditor = null;
        if (entry.readable | success) {
            entry.readable = true;
            journalWriter.write(CLEAN + ' ' + entry.key + entry.getLengths() + '\n');
            if (success) {
                entry.sequenceNumber = nextSequenceNumber++;
            }
        } else {
            lruEntries.remove(entry.key);
            journalWriter.write(REMOVE + ' ' + entry.key + '\n');
        }

        if (size > maxSize || journalRebuildRequired()) {
            executorService.submit(cleanupCallable);//开启线程整理缓存
        }
    }



    //通过 SnapShot可以得到输入流，进而得到缓存图片
    public final class Snapshot implements Closeable{

        private final String key;
        private final long sequenceNumber;
        private final InputStream[] ins;

        private Snapshot(String key, long sequenceNumber, InputStream[] ins) {
            this.key = key;
            this.sequenceNumber = sequenceNumber;
            this.ins = ins;
        }
        public Disk.Editor edit() throws IOException {
            return Disk.this.edit(key, sequenceNumber);
        }

        public InputStream getInputStream(int index) {
            return ins[index];
        }

        public String getString(int index) throws IOException {
            return inputStreamToString(getInputStream(index));
        }
        @Override
        public void close() throws IOException {

        }
    }
    private static String inputStreamToString(InputStream in) throws IOException {
        return readFully(new InputStreamReader(in, UTF_8));
    }
}
