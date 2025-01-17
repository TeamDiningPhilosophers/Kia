package dining.philosophers.com.kia;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Environment;
import android.os.VibrationEffect;
import android.os.Vibrator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;

public class Utils {

    static void  saveImage(Context ctx, Bitmap bmp) throws IOException {
        String path = ctx.getExternalFilesDir(Environment.DIRECTORY_PICTURES).toString()
                + "/img-" + (new Date()).getTime() + ".jpg";

        FileOutputStream outputStream = new FileOutputStream(new File(path));
        bmp.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
        outputStream.flush();
        outputStream.close();
        bmp.recycle();
    }
    static void playReload(Context context) {
        MediaPlayer mp = MediaPlayer.create(context, R.raw.reload);
        mp.start();
        mp.setOnCompletionListener(MediaPlayer::release);
    }
    static void playFireEmpty(Context context) {
        MediaPlayer mp = MediaPlayer.create(context, R.raw.fire_empty);
        mp.start();
        mp.setOnCompletionListener(MediaPlayer::release);
    }

    static void playFireNormal(Context context) {
        MediaPlayer mp = MediaPlayer.create(context, R.raw.fire_normal);
        mp.start();
        mp.setOnCompletionListener(MediaPlayer::release);
    }
}
