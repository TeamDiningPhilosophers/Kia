package dining.philosophers.com.kia;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.HandlerThread;
import android.view.PixelCopy;
import android.widget.Toast;

import com.google.ar.core.Config;
import com.google.ar.core.Session;
import com.google.ar.sceneform.ArSceneView;
import com.google.ar.sceneform.ux.ArFragment;

import java.io.IOException;

public class CustomArFragment extends ArFragment {

    @Override
    protected Config getSessionConfiguration(Session session) {
        getPlaneDiscoveryController().setInstructionView(null);
        Config config = super.getSessionConfiguration(session);
        config.setCloudAnchorMode(Config.CloudAnchorMode.ENABLED);
        return config;
    }

    interface OnBitmapCapturedListener {
        void onBitmapCaptured(Bitmap bitmap);
    }

    public void captureBitmap(OnBitmapCapturedListener obcl, boolean save) {
        ArSceneView view = getArSceneView();

        // Create a bitmap the size of the scene view.
        Bitmap bitmap = Bitmap.createBitmap(300, 600,
                Bitmap.Config.ARGB_8888);
        // Create a handler thread to offload the processing of the image.
        final HandlerThread handlerThread = new HandlerThread("PixelCopier");
        handlerThread.start();
        // Make the request to copy.
        PixelCopy.request(view, bitmap, new PixelCopy.OnPixelCopyFinishedListener() {
            @Override
            public void onPixelCopyFinished(int copyResult) {
                if (copyResult == PixelCopy.SUCCESS) {
                    if (save) {
                        try {
                            Utils.saveImage(getActivity(), Bitmap.createBitmap(bitmap, 0, 150, 300, 300));
                        } catch (IOException e) {
                            Toast.makeText(getActivity(), e.toString(),
                                    Toast.LENGTH_LONG).show();
                            return;
                        }
                    } else if (obcl != null) {
                        obcl.onBitmapCaptured(Bitmap.createBitmap(bitmap, 0, 150, 300, 300));
                    }
                } else {
                    Toast toast = Toast.makeText(getActivity(),
                            "Failed to copyPixels: " + copyResult, Toast.LENGTH_LONG);
                    toast.show();
                }
                handlerThread.quitSafely();
            }
        }, new Handler(handlerThread.getLooper()));
    }
}
