/**
 * Created by : yds
 * Time: 2023-02-15 14:10
 */
package com.crystallake.textrecognizeactivity;

import androidx.annotation.NonNull;

public interface RecognizeListener<T> {
    void onSuccess(@NonNull T results);
    void onFailure(@NonNull Exception e);
}
