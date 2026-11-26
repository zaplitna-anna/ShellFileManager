package com.shell.icons;

import com.shell.model.FileSystemItem;

import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.scene.layout.StackPane;

public final class FileIconProvider {

    private static final String FOLDER_ICON =
            "M2 5 H9 L11 7 H22 V20 H2 Z";

    private static final String FILE_ICON =
            "M5 2 H14 L20 8 V22 H5 Z";

    private static final String IMAGE_ICON =
            "M2 3 H22 V21 H2 Z " +
                    "M4 18 L10 10 L14 15 L17 12 L20 18 Z";

    private static final String ARCHIVE_ICON =
            "M3 3 H21 V21 H3 Z " +
                    "M7 3 V7 H17 V3 " +
                    "M10 12 H14";

    private static final String APPLICATION_ICON =
            "M2 3 H22 V21 H2 Z " +
                    "M2 8 H22";

    private FileIconProvider() {
    }


    public static Node createIcon(FileSystemItem item) {
        if (item.isDirectory()) {
            return createSvgIcon(
                    FOLDER_ICON,
                    "#b69acd"
            );
        }

        return switch (item.getExtension()) {
            case "pdf" -> createSvgIcon(FILE_ICON, "#c56b80");

            case "doc", "docx" -> createSvgIcon(FILE_ICON, "#7188bd");

            case "xls", "xlsx", "csv" -> createSvgIcon(FILE_ICON, "#699a80");

            case "ppt", "pptx" -> createSvgIcon(FILE_ICON, "#cc9873");

            case "jpg", "jpeg", "png", "gif",
                 "bmp", "webp", "svg" -> createSvgIcon(IMAGE_ICON, "#a27bbf");

            case "zip", "rar", "7z", "tar", "gz" -> createSvgIcon(ARCHIVE_ICON, "#b49a71");

            case "exe", "msi", "bat", "cmd" -> createSvgIcon(APPLICATION_ICON, "#716783");

            case "txt", "log", "md" -> createSvgIcon(FILE_ICON, "#8c809f");

            default -> createSvgIcon(FILE_ICON, "#a49bb3");
        };
    }

    private static Node createSvgIcon(
            String content,
            String color
    ) {
        SVGPath icon = new SVGPath();

        icon.setContent(content);
        icon.setFill(Color.web(color, 0.18));
        icon.setStroke(Color.web(color));
        icon.setStrokeWidth(1.5);

        icon.setScaleX(0.7);
        icon.setScaleY(0.7);

        StackPane container = new StackPane(icon);

        container.setMinSize(18, 18);
        container.setPrefSize(18, 18);
        container.setMaxSize(18, 18);

        return container;
    }
}
