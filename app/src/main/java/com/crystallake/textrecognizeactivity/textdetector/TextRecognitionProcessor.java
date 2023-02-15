/*
 * Copyright 2020 Google LLC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.crystallake.textrecognizeactivity.textdetector;

import android.content.Context;
import android.graphics.Point;
import android.util.Log;

import androidx.annotation.NonNull;

import com.crystallake.textrecognizeactivity.RecognizeListener;
import com.crystallake.textrecognizeactivity.VisionProcessorBase;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.TextRecognizerOptionsInterface;


/**
 * Processor for the text detector demo.
 */
public class TextRecognitionProcessor extends VisionProcessorBase<Text> {

    private static final String TAG = "TextRecProcessor";

    private final TextRecognizer textRecognizer;
    private RecognizeListener<Text> mRecognizeListener;

    public TextRecognitionProcessor(
            Context context, TextRecognizerOptionsInterface textRecognizerOptions) {
        super(context);
        textRecognizer = TextRecognition.getClient(textRecognizerOptions);
    }

    @Override
    public void stop() {
        super.stop();
        textRecognizer.close();
    }

    public TextRecognitionProcessor setRecognizeListener(RecognizeListener<Text> mRecognizeListener){
        this.mRecognizeListener = mRecognizeListener;
        return this;
    }

    @Override
    protected Task<Text> detectInImage(InputImage image) {
        return textRecognizer.process(image);
    }

    @Override
    protected void onSuccess(@NonNull Text text) {
        Log.d(TAG, "On-device Text detection successful");
        if (mRecognizeListener != null) {
            mRecognizeListener.onSuccess(text);
        }

    }

    @Override
    protected void onFailure(@NonNull Exception e) {
        Log.w(TAG, "Text detection failed." + e);
        if (mRecognizeListener != null) {
            mRecognizeListener.onFailure(e);
        }
    }

}
