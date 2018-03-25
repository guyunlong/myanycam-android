package gyl.cam;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.BufferOverflowException;

public class CircularByteBuffer {
	
	private final static int DEFAULT_SIZE = 10240;

	
	public final static int INFINITE_SIZE = -1;

	
	protected byte[] buffer;
	
	protected volatile int readPosition = 0;
	
	protected volatile int writePosition = 0;
	
	protected volatile int markPosition = 0;
	
	protected volatile int markSize = 0;
	
	protected volatile boolean infinite = false;
	
	protected boolean blockingWrite = true;
	
	protected InputStream in = new CircularByteBufferInputStream();
	
	protected boolean inputStreamClosed = false;
	
	protected OutputStream out = new CircularByteBufferOutputStream();
	
	protected boolean outputStreamClosed = false;

	
	public void clear() {
		synchronized (this) {
			readPosition = 0;
			writePosition = 0;
			markPosition = 0;
			outputStreamClosed = false;
			inputStreamClosed = false;
		}
	}

	
	public OutputStream getOutputStream() {
		return out;
	}

	
	public InputStream getInputStream() {
		return in;
	}

	
	public int getAvailable() {
		synchronized (this) {
			return available();
		}
	}

	
	public int getSpaceLeft() {
		synchronized (this) {
			return spaceLeft();
		}
	}

	
	public int getSize() {
		synchronized (this) {
			return buffer.length;
		}
	}

	
	private void resize() {
		byte[] newBuffer = new byte[buffer.length * 2];
		int marked = marked();
		int available = available();
		if (markPosition <= writePosition) {
			// any space between the mark and
			// the first write needs to be saved.
			// In this case it is all in one piece.
			int length = writePosition - markPosition;
			System.arraycopy(buffer, markPosition, newBuffer, 0, length);
		} else {
			int length1 = buffer.length - markPosition;
			System.arraycopy(buffer, markPosition, newBuffer, 0, length1);
			int length2 = writePosition;
			System.arraycopy(buffer, 0, newBuffer, length1, length2);
		}
		buffer = newBuffer;
		markPosition = 0;
		readPosition = marked;
		writePosition = marked + available;
	}

	
	private int spaceLeft() {
		if (writePosition < markPosition) {
			// any space between the first write and
			// the mark except one byte is available.
			// In this case it is all in one piece.
			return (markPosition - writePosition - 1);
		}
		// space at the beginning and end.
		return ((buffer.length - 1) - (writePosition - markPosition));
	}

	
	private int available() {
		if (readPosition <= writePosition) {
			// any space between the first read and
			// the first write is available. In this case i
			// is all in one piece.
			return (writePosition - readPosition);
		}
		// space at the beginning and end.
		return (buffer.length - (readPosition - writePosition));
	}

	
	private int marked() {
		if (markPosition <= readPosition) {
			// any space between the markPosition and
			// the first write is marked. In this case i
			// is all in one piece.
			return (readPosition - markPosition);
		}
		// space at the beginning and end.
		return (buffer.length - (markPosition - readPosition));
	}

	
	private void ensureMark() {
		if (marked() > markSize) {
			markPosition = readPosition;
			markSize = 0;
		}
	}

	
	public CircularByteBuffer() {
		this(DEFAULT_SIZE, true);
	}

	
	public CircularByteBuffer(int size) {
		this(size, true);
	}

	
	public CircularByteBuffer(boolean blockingWrite) {
		this(DEFAULT_SIZE, blockingWrite);
	}

	
	public CircularByteBuffer(int size, boolean blockingWrite) {
		if (size == INFINITE_SIZE) {
			buffer = new byte[DEFAULT_SIZE];
			infinite = true;
		} else {
			buffer = new byte[size];
			infinite = false;
		}
		this.blockingWrite = blockingWrite;
	}

	
	protected class CircularByteBufferInputStream extends InputStream {

		
		@Override
		public int available() throws IOException {
			synchronized (CircularByteBuffer.this) {
				if (inputStreamClosed)
					throw new IOException(
							"InputStream has been closed, it is not ready.");
				return (CircularByteBuffer.this.available());
			}
		}

		
		@Override
		public void close() throws IOException {
			synchronized (CircularByteBuffer.this) {
				inputStreamClosed = true;
			}
		}

		
		@Override
		public void mark(int readAheadLimit) {
			synchronized (CircularByteBuffer.this) {
				// if (inputStreamClosed) throw new
				// IOException("InputStream has been closed; cannot mark a closed InputStream.");
				if (buffer.length - 1 > readAheadLimit) {
					markSize = readAheadLimit;
					markPosition = readPosition;
				}
			}
		}

		
		@Override
		public boolean markSupported() {
			return true;
		}

		
		@Override
		public int read() throws IOException {
			while (true) {
				synchronized (CircularByteBuffer.this) {
					if (inputStreamClosed)
						throw new IOException(
								"InputStream has been closed; cannot read from a closed InputStream.");
					int available = CircularByteBuffer.this.available();
					if (available > 0) {
						int result = buffer[readPosition] & 0xff;
						readPosition++;
						if (readPosition == buffer.length) {
							readPosition = 0;
						}
						ensureMark();
						return result;
					} else if (outputStreamClosed) {
						return -1;
					}
				}
				try {
					Thread.sleep(100);
				} catch (Exception x) {
					throw new IOException(
							"Blocking read operation interrupted.");
				}
			}
		}

		
		@Override
		public int read(byte[] cbuf) throws IOException {
			return read(cbuf, 0, cbuf.length);
		}

		
		@Override
		public int read(byte[] cbuf, int off, int len) throws IOException {
			while (true) {
				synchronized (CircularByteBuffer.this) {
					if (inputStreamClosed)
						throw new IOException(
								"InputStream has been closed; cannot read from a closed InputStream.");
					int available = CircularByteBuffer.this.available();
					if (available > 0) {
						int length = Math.min(len, available);
						int firstLen = Math.min(length, buffer.length
								- readPosition);
						int secondLen = length - firstLen;
						System.arraycopy(buffer, readPosition, cbuf, off,
								firstLen);
						if (secondLen > 0) {
							System.arraycopy(buffer, 0, cbuf, off + firstLen,
									secondLen);
							readPosition = secondLen;
						} else {
							readPosition += length;
						}
						if (readPosition == buffer.length) {
							readPosition = 0;
						}
						ensureMark();
						return length;
					} else if (outputStreamClosed) {
						return -1;
					}
				}
				try {
					Thread.sleep(100);
				} catch (Exception x) {
					throw new IOException(
							"Blocking read operation interrupted.");
				}
			}
		}

		
		@Override
		public void reset() throws IOException {
			synchronized (CircularByteBuffer.this) {
				if (inputStreamClosed)
					throw new IOException(
							"InputStream has been closed; cannot reset a closed InputStream.");
				readPosition = markPosition;
			}
		}

		
		@Override
		public long skip(long n) throws IOException, IllegalArgumentException {
			while (true) {
				synchronized (CircularByteBuffer.this) {
					if (inputStreamClosed)
						throw new IOException(
								"InputStream has been closed; cannot skip bytes on a closed InputStream.");
					int available = CircularByteBuffer.this.available();
					if (available > 0) {
						int length = Math.min((int) n, available);
						int firstLen = Math.min(length, buffer.length
								- readPosition);
						int secondLen = length - firstLen;
						if (secondLen > 0) {
							readPosition = secondLen;
						} else {
							readPosition += length;
						}
						if (readPosition == buffer.length) {
							readPosition = 0;
						}
						ensureMark();
						return length;
					} else if (outputStreamClosed) {
						return 0;
					}
				}
				try {
					Thread.sleep(100);
				} catch (Exception x) {
					throw new IOException(
							"Blocking read operation interrupted.");
				}
			}
		}
	}

	
	protected class CircularByteBufferOutputStream extends OutputStream {

		
		@Override
		public void close() throws IOException {
			synchronized (CircularByteBuffer.this) {
				if (!outputStreamClosed) {
					flush();
				}
				outputStreamClosed = true;
			}
		}

		
		@Override
		public void flush() throws IOException {
			synchronized (CircularByteBuffer.this) {
				if (outputStreamClosed)
					throw new IOException(
							"OutputStream has been closed; cannot flush a closed OutputStream.");
				if (inputStreamClosed)
					throw new IOException(
							"Buffer closed by inputStream; cannot flush.");
			}
			// this method needs to do nothing
		}

		
		@Override
		public void write(byte[] cbuf) throws IOException {
			write(cbuf, 0, cbuf.length);
		}

		
		@Override
		public void write(byte[] cbuf, int off, int len) throws IOException {
			while (len > 0) {
				synchronized (CircularByteBuffer.this) {
					if (outputStreamClosed)
						throw new IOException(
								"OutputStream has been closed; cannot write to a closed OutputStream.");
					if (inputStreamClosed)
						throw new IOException(
								"Buffer closed by InputStream; cannot write to a closed buffer.");
					int spaceLeft = spaceLeft();
					while (infinite && spaceLeft < len) {
						resize();
						spaceLeft = spaceLeft();
					}
					if (!blockingWrite && spaceLeft < len)
						throw new BufferOverflowException();
					int realLen = Math.min(len, spaceLeft);
					int firstLen = Math.min(realLen, buffer.length
							- writePosition);
					int secondLen = Math.min(realLen - firstLen, buffer.length
							- markPosition - 1);
					int written = firstLen + secondLen;
					if (firstLen > 0) {
						System.arraycopy(cbuf, off, buffer, writePosition,
								firstLen);
					}
					if (secondLen > 0) {
						System.arraycopy(cbuf, off + firstLen, buffer, 0,
								secondLen);
						writePosition = secondLen;
					} else {
						writePosition += written;
					}
					if (writePosition == buffer.length) {
						writePosition = 0;
					}
					off += written;
					len -= written;
				}
				if (len > 0) {
					try {
						Thread.sleep(100);
					} catch (Exception x) {
						throw new IOException(
								"Waiting for available space in buffer interrupted.");
					}
				}
			}
		}

		
		@Override
		public void write(int c) throws IOException {
			boolean written = false;
			while (!written) {
				synchronized (CircularByteBuffer.this) {
					if (outputStreamClosed)
						throw new IOException(
								"OutputStream has been closed; cannot write to a closed OutputStream.");
					if (inputStreamClosed)
						throw new IOException(
								"Buffer closed by InputStream; cannot write to a closed buffer.");
					int spaceLeft = spaceLeft();
					while (infinite && spaceLeft < 1) {
						resize();
						spaceLeft = spaceLeft();
					}
					if (!blockingWrite && spaceLeft < 1)
						throw new BufferOverflowException();
					if (spaceLeft > 0) {
						buffer[writePosition] = (byte) (c & 0xff);
						writePosition++;
						if (writePosition == buffer.length) {
							writePosition = 0;
						}
						written = true;
					}
				}
				if (!written) {
					try {
						Thread.sleep(100);
					} catch (Exception x) {
						throw new IOException(
								"Waiting for available space in buffer interrupted.");
					}
				}
			}
		}
	}
}
