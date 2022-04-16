package net.oldschoolminecraft.iph.util;

import org.apache.commons.collections4.MapIterator;
import org.apache.commons.collections4.map.LRUMap;

import java.util.ArrayList;

public class MemoryCache<K, T> {

    private final long timeToLive;
    private final LRUMap mapCache;

    protected class CacheObject
    {
        public long lastAccessed = System.currentTimeMillis();
        public T value;

        protected CacheObject(T value)
        {
            this.value = value;
        }
    }

    public MemoryCache(long timeToLive, final long timerInterval, int maxItems)
    {
        this.timeToLive = timeToLive * 1000;
        mapCache = new LRUMap(maxItems);

        if (this.timeToLive > 0 && timerInterval > 0)
        {
            Thread t = new Thread(() ->
            {
                while (true)
                {
                    try
                    {
                        Thread.sleep(timerInterval * 1000);
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                    cleanup();
                }
            });

            t.setDaemon(true);
            t.start();
        }
    }

    public void put(K key, T value)
    {
        synchronized (mapCache)
        {
            mapCache.put(key, new CacheObject(value));
        }
    }

    public T get(K key)
    {
        synchronized (mapCache)
        {
            CacheObject c;
            c = (CacheObject) mapCache.get(key);

            if (c == null) return null;
            else {
                c.lastAccessed = System.currentTimeMillis();
                return c.value;
            }
        }
    }

    public void remove(K key)
    {
        synchronized (mapCache)
        {
            mapCache.remove(key);
        }
    }

    public int size()
    {
        synchronized (mapCache)
        {
            return mapCache.size();
        }
    }

    public void cleanup()
    {
        long now = System.currentTimeMillis();
        ArrayList<K> deleteKey = null;

        synchronized (mapCache)
        {
            MapIterator itr = mapCache.mapIterator();
            deleteKey = new ArrayList<K>((mapCache.size() / 2) + 1);
            K key = null;
            CacheObject c = null;

            while (itr.hasNext())
            {
                key = (K) itr.next();
                c = (CacheObject) itr.getValue();

                if (c != null && (now > (timeToLive + c.lastAccessed))) deleteKey.add(key);
            }
        }

        for (K key : deleteKey)
        {
            synchronized (mapCache)
            {
                mapCache.remove(key);
            }

            Thread.yield();
        }
    }
}
