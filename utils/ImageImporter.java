// File: ImageImporter.java
package javaproject.utils;

import java.io.File;

import javafx.scene.canvas.Canvas;
import javafx.scene.image.Image;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class ImageImporter {

    public static void importImage(Stage primaryStage, Canvas canvas, ImageSetter imageSetter, Runnable redraw) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Import Map Image");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
        );
        File file = fileChooser.showOpenDialog(primaryStage);

        if (file != null) {
            try {
                Image backgroundImage = new Image(file.toURI().toString());
                canvas.setWidth(backgroundImage.getWidth());
                canvas.setHeight(backgroundImage.getHeight());
                imageSetter.setImage(backgroundImage); // update the image in the caller
                redraw.run(); // trigger canvas redraw
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public interface ImageSetter {
        void setImage(Image image);
    }
}
