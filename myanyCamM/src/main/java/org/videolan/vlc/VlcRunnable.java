
package org.videolan.vlc;

public abstract class VlcRunnable implements Runnable {
    private final Object user;

    public VlcRunnable() {
        this.user = null;
    }

    public VlcRunnable(Object o) {
        this.user = o;
    }

    public abstract void run(Object o);

    @Override
    public void run() {
        run(user);
    }
}
