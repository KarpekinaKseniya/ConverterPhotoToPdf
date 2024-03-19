import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Image;
import com.itextpdf.text.RectangleReadOnly;
import com.itextpdf.text.pdf.PdfWriter;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class Converter {

  private static final String WIDTH_KEY = "Width";
  private static final String HEIGHT_KEY = "Height";
  private static final Float MAX_PAGE_SIZE = 14400f;

  public static void main(final String[] args) throws IOException, DocumentException {
    final Set<String> folders = listFoldersOrFiles(args[0], Files::isDirectory);
    final Map<String, Set<String>> folderToFile = new HashMap<>();
    for (final String folder : folders) {
      final String path = args[0] + '/' + folder;
      final Set<String> files =
          listFoldersOrFiles(path, file -> !Files.isDirectory(file)).stream()
              .sorted()
              .collect(Collectors.toCollection(LinkedHashSet::new));
      Set<String> paths = new LinkedHashSet<>();
      for (final String file : files) {
        final String newPath = path + '/' + file;
        paths.add(newPath);
      }
      folderToFile.put(folder, paths);
    }

    for (final String folder : folderToFile.keySet()) {
      final Set<String> images = folderToFile.get(folder);
      final Map<String, Float> size = getMaxImageSize(images);
      Document document =
          new Document(
              new RectangleReadOnly(size.get(WIDTH_KEY), size.get(HEIGHT_KEY)),
              0.0F,
              0.0F,
              0.0F,
              0.0F);
      String output = args[0] + "/" + folder + ".pdf";
      FileOutputStream fos = new FileOutputStream(output);
      PdfWriter writer = PdfWriter.getInstance(document, fos);
      writer.open();
      document.open();
      for (final String image : images) {
        document.add(Image.getInstance(((image))));
      }
      document.close();
      writer.close();
      System.out.println("Complete folder " + folder);
    }
    System.out.println("Converting complete successful!");
  }

  private static Map<String, Float> getMaxImageSize(final Set<String> images) throws IOException {
    float maxWidth = Float.MIN_VALUE;
    float maxHeight = Float.MIN_VALUE;
    for (final String image : images) {
      final BufferedImage bi = ImageIO.read(new File(image));
      final int width = bi.getWidth();
      final int height = bi.getHeight();
      if (maxHeight < height) {
        maxHeight = height + 50f;
      }
      if (maxWidth < width) {
        maxWidth = width + 50f;
      }
    }
    final Map<String, Float> map = new HashMap<>();
    map.put(WIDTH_KEY, maxWidth > MAX_PAGE_SIZE ? MAX_PAGE_SIZE : maxWidth);
    map.put(HEIGHT_KEY, maxHeight > MAX_PAGE_SIZE ? MAX_PAGE_SIZE : maxHeight);
    return map;
  }

  private static Set<String> listFoldersOrFiles(final String dir, final Predicate<Path> filter)
      throws IOException {
    try (Stream<Path> stream = Files.list(Paths.get(dir))) {
      return stream
          .filter(filter)
          .map(Path::getFileName)
          .map(Path::toString)
          .collect(Collectors.toSet());
    }
  }
}
