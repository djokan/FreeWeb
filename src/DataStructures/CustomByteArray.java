package DataStructures;

import java.util.Arrays;


public class CustomByteArray {
    byte[] array;

    public CustomByteArray(byte[] array1) {
        this.array = array1;
    }

    public byte[] getArray() {
        return array;
    }

    public int hashCode()
    {
        return Arrays.hashCode(array);
    }
    public boolean equals(Object other)
    {
        if (other == null) return false;
        if (other == this) return true;
        if (!(other instanceof CustomByteArray))return false;
        CustomByteArray otherMyClass = (CustomByteArray)other;
        return Arrays.equals(this.array, otherMyClass.array);
    }
}
