package com.crystallake.textrecognizeactivity;

import static java.lang.Math.max;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.crystallake.textrecognizeactivity.textdetector.TextRecognitionProcessor;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.IOException;

public class StillImageActivity extends AppCompatActivity {

    private static final String TAG = "StillImageActivity";
    private static final String TEXT_RECOGNITION_LATIN = "Text Recognition Latin";
    private static final String TEXT_RECOGNITION_CHINESE = "Text Recognition Chinese (Beta)";

    private static final int REQUEST_CHOOSE_IMAGE = 1002;

    private static final String SIZE_SCREEN = "w:screen"; // Match screen width
    private static final String SIZE_1024_768 = "w:1024"; // ~1024*768 in a normal ratio
    private static final String SIZE_640_480 = "w:640"; // ~640*480 in a normal ratio
    private static final String SIZE_ORIGINAL = "w:original"; // Original image size

    private String selectedMode = TEXT_RECOGNITION_CHINESE;
    private String selectedSize = SIZE_SCREEN;

    private Uri imageUri;
    private int imageMaxWidth;
    private int imageMaxHeight;

    boolean isLandScape;
    private ImageView preview;

    private VisionImageProcessor imageProcessor;

    private TextView recognizeMsg;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_still_image);

        isLandScape =
                (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE);

        preview = findViewById(R.id.preview);
        recognizeMsg = findViewById(R.id.scan_msg);
        findViewById(R.id.select_image_button)
                .setOnClickListener(
                        view -> {
                            // Menu for selecting either: a) take new photo b) select from existing
                            PopupMenu popup = new PopupMenu(StillImageActivity.this, view);
                            popup.setOnMenuItemClickListener(
                                    menuItem -> {
                                        int itemId = menuItem.getItemId();
                                        if (itemId == R.id.select_images_from_local) {
                                            startChooseImageIntentForResult();
                                            return true;
                                        } else if (itemId == R.id.take_photo_using_camera) {
//                                            startCameraIntentForResult();
                                            return true;
                                        }
                                        return false;
                                    });
                            MenuInflater inflater = popup.getMenuInflater();
                            inflater.inflate(R.menu.camera_button_menu, popup.getMenu());
                            popup.show();
                        });

        View rootView = findViewById(R.id.root);
        rootView.getViewTreeObserver()
                .addOnGlobalLayoutListener(
                        new ViewTreeObserver.OnGlobalLayoutListener() {
                            @Override
                            public void onGlobalLayout() {
                                rootView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                                imageMaxWidth = rootView.getWidth();
                                imageMaxHeight = rootView.getHeight() - findViewById(R.id.control).getHeight();
                                if (SIZE_SCREEN.equals(selectedSize)) {
                                    tryReloadAndDetectInImage();
                                }
                            }
                        });
    }

    private void startChooseImageIntentForResult() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), REQUEST_CHOOSE_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CHOOSE_IMAGE && resultCode == RESULT_OK) {
            // In this case, imageUri is returned by the chooser, save it.
            imageUri = data.getData();
            tryReloadAndDetectInImage();
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void tryReloadAndDetectInImage() {
        Log.d(TAG, "Try reload and detect image");
        try {
            if (imageUri == null) {
                return;
            }

            if (SIZE_SCREEN.equals(selectedSize) && imageMaxWidth == 0) {
                // UI layout has not finished yet, will reload once it's ready.
                return;
            }

            Bitmap imageBitmap = BitmapUtils.getBitmapFromContentUri(getContentResolver(), imageUri);
            if (imageBitmap == null) {
                return;
            }

            // Clear the overlay first

            Bitmap resizedBitmap;
            if (selectedSize.equals(SIZE_ORIGINAL)) {
                resizedBitmap = imageBitmap;
            } else {
                // Get the dimensions of the image view
                Pair<Integer, Integer> targetedSize = getTargetedWidthHeight();

                // Determine how much to scale down the image
                float scaleFactor =
                        max(
                                (float) imageBitmap.getWidth() / (float) targetedSize.first,
                                (float) imageBitmap.getHeight() / (float) targetedSize.second);

                resizedBitmap =
                        Bitmap.createScaledBitmap(
                                imageBitmap,
                                (int) (imageBitmap.getWidth() / scaleFactor),
                                (int) (imageBitmap.getHeight() / scaleFactor),
                                true);
            }

            preview.setImageBitmap(resizedBitmap);

            if (imageProcessor != null) {
                imageProcessor.processBitmap(resizedBitmap,this);
            } else {
                Log.e(TAG, "Null imageProcessor, please check adb logs for imageProcessor creation error");
            }
        } catch (IOException e) {
            Log.e(TAG, "Error retrieving saved image");
            imageUri = null;
        }
    }

    private Pair<Integer, Integer> getTargetedWidthHeight() {
        int targetWidth;
        int targetHeight;

        switch (selectedSize) {
            case SIZE_SCREEN:
                targetWidth = imageMaxWidth;
                targetHeight = imageMaxHeight;
                break;
            case SIZE_640_480:
                targetWidth = isLandScape ? 640 : 480;
                targetHeight = isLandScape ? 480 : 640;
                break;
            case SIZE_1024_768:
                targetWidth = isLandScape ? 1024 : 768;
                targetHeight = isLandScape ? 768 : 1024;
                break;
            default:
                throw new IllegalStateException("Unknown size");
        }

        return new Pair<>(targetWidth, targetHeight);
    }

    private void createImageProcessor(){
        if (imageProcessor != null) {
            imageProcessor.stop();
        }
        try {
            switch (selectedMode){
                case TEXT_RECOGNITION_LATIN:
                    if (imageProcessor != null) {
                        imageProcessor.stop();
                    }
                    imageProcessor =
                            new TextRecognitionProcessor(this, new TextRecognizerOptions.Builder().build())
                                    .setRecognizeListener(new RecognizeListener<Text>() {
                                        @Override
                                        public void onSuccess(@NonNull Text results) {
                                            StringBuilder sb = new StringBuilder();
                                            for (Text.TextBlock textBlock : results.getTextBlocks()) {
                                                sb.append(textBlock.getText()).append("\n");
                                            }
                                            recognizeMsg.setText(sb.toString());
                                        }

                                        @Override
                                        public void onFailure(@NonNull Exception e) {

                                        }
                                    });
                    break;
                case TEXT_RECOGNITION_CHINESE:
                    if (imageProcessor != null) {
                        imageProcessor.stop();
                    }
                    imageProcessor =
                            new TextRecognitionProcessor(
                                    this, new ChineseTextRecognizerOptions.Builder().build()).setRecognizeListener(new RecognizeListener<Text>() {
                                @Override
                                public void onSuccess(@NonNull Text results) {
                                    StringBuilder sb = new StringBuilder();
                                    for (Text.TextBlock textBlock : results.getTextBlocks()) {
                                        sb.append(textBlock.getText()).append("\n");
                                    }
                                    recognizeMsg.setText(sb.toString());
                                }

                                @Override
                                public void onFailure(@NonNull Exception e) {

                                }
                            });
                    break;
            }
        }catch (Exception e){
            Log.e(TAG, "Can not create image processor: " + selectedMode, e);
            Toast.makeText(
                            getApplicationContext(),
                            "Can not create image processor: " + e.getMessage(),
                            Toast.LENGTH_LONG)
                    .show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        createImageProcessor();
        tryReloadAndDetectInImage();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (imageProcessor != null) {
            imageProcessor.stop();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (imageProcessor != null) {
            imageProcessor.stop();
        }
    }
}