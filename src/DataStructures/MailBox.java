package DataStructures;

import java.util.concurrent.Semaphore;

public class MailBox {
	Semaphore sem;
	public Semaphore getSem() {
		return sem;
	}
	byte[] data;
	public byte[] getData() {
		return data;
	}
	public void setData(byte[] data) {
		this.data = data;
	}
	public MailBox(Semaphore s1)
	{
		sem=s1;
	}
}
