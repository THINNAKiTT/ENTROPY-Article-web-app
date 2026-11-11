package util;

import java.io.Serializable;

public record Picture(byte[] data, String type, long modified) implements Serializable {
}
