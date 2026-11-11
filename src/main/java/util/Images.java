package util;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

public final class Images {

    private Images() {}

    public static final int MAX_UPLOAD_BYTES = 5 * 1024 * 1024;
    public static final int AVATAR_SIZE = 256;
    public static final int ARTICLE_MAX_SIDE = 1600;

    public static Picture avatar(byte[] data) throws IOException {
        BufferedImage source = read(data);
        int side = Math.min(source.getWidth(), source.getHeight());
        BufferedImage square = source.getSubimage((source.getWidth() - side) / 2, (source.getHeight() - side) / 2, side, side);
        int target = Math.min(side, AVATAR_SIZE);
        return new Picture(write(scale(square, target, target), "png"), "image/png", 0);
    }

    public static Picture article(byte[] data) throws IOException {
        String format = format(data);
        BufferedImage source = read(data);
        int longest = Math.max(source.getWidth(), source.getHeight());
        if (longest <= ARTICLE_MAX_SIDE || format.equals("gif")) {
            return new Picture(data, "image/" + format, 0);
        }
        double ratio = (double) ARTICLE_MAX_SIDE / longest;
        int width = Math.max(1, (int) Math.round(source.getWidth() * ratio));
        int height = Math.max(1, (int) Math.round(source.getHeight() * ratio));
        return new Picture(write(scale(source, width, height), format), "image/" + format, 0);
    }

    private static BufferedImage read(byte[] data) throws IOException {
        format(data);
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(data));
        if (image == null || image.getWidth() < 1 || image.getHeight() < 1) {
            throw new IOException("That file is not a picture.");
        }
        return image;
    }

    private static String format(byte[] data) throws IOException {
        try (ImageInputStream in = ImageIO.createImageInputStream(new ByteArrayInputStream(data))) {
            Iterator<ImageReader> readers = in == null ? null : ImageIO.getImageReaders(in);
            if (readers != null && readers.hasNext()) {
                String name = readers.next().getFormatName().toLowerCase();
                if (name.equals("jpeg") || name.equals("jpg")) {
                    return "jpeg";
                }
                if (name.equals("png") || name.equals("gif")) {
                    return name;
                }
            }
        }
        throw new IOException("Use a PNG, JPEG or GIF picture.");
    }

    private static BufferedImage scale(BufferedImage source, int width, int height) {
        BufferedImage out = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(source, 0, 0, width, height, null);
        g.dispose();
        return out;
    }

    private static byte[] write(BufferedImage image, String format) throws IOException {
        BufferedImage toWrite = image;
        if (format.equals("jpeg")) {
            toWrite = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D g = toWrite.createGraphics();
            g.drawImage(image, 0, 0, java.awt.Color.WHITE, null);
            g.dispose();
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        if (!ImageIO.write(toWrite, format, out)) {
            throw new IOException("Could not save that picture.");
        }
        return out.toByteArray();
    }
}
