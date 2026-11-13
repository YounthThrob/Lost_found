package com.lostfound.storage;

/** 图片二进制存储抽象。MVP 落地为本地文件系统，后续可替换为对象存储。 */
public interface Storage {

    void put(String key, byte[] content, String contentType);

    byte[] get(String key);

    void delete(String key);
}
