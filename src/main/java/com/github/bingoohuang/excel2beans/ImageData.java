package com.github.bingoohuang.excel2beans;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.poi.ss.usermodel.PictureData;

@Data @NoArgsConstructor @AllArgsConstructor
public class ImageData implements PictureData {
    private byte[] data;
    private String suggestFileExtension;
    private String mimeType;
    private int pictureType;

    @Override public byte[] getData() {
        return data;
    }

    @Override public String suggestFileExtension() {
        return suggestFileExtension;
    }

    @Override public String getMimeType() {
        return mimeType;
    }

    @Override public int getPictureType() {
        return pictureType;
    }
}