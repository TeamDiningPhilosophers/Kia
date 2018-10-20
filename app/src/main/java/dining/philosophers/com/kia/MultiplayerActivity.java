package dining.philosophers.com.kia;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;

import com.google.ar.core.Anchor;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.Renderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;

import dining.philosophers.com.kia.ml.TFMobile;

public class MultiplayerActivity extends AppCompatActivity {

    private enum AppAnchorState {
        NONE,
        HOSTING,
        HOSTED,
        RESOLVING,
        RESOLVED
    }

    private AppAnchorState appAnchorState = AppAnchorState.NONE;

    private SnackbarHelper snackbarHelper = new SnackbarHelper();
    private CustomArFragment fragment;
    private Anchor cloudAnchor;

    private StorageManager storageManager;

    private TFMobile tfMobile;

    private Button shootButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_multiplayer);

        fragment = (CustomArFragment) getSupportFragmentManager().findFragmentById(R.id.sceneform_fragment);
        fragment.getPlaneDiscoveryController().hide();

        tfMobile = new TFMobile(this);

        Button clearButton = findViewById(R.id.clear_button);
        clearButton.setOnClickListener(view -> setCloudAnchor(null));

        storageManager = new StorageManager(this);

        fragment.setOnTapArPlaneListener((HitResult hitResult, Plane plane, MotionEvent motionEvent) -> {

                    if (plane.getType() != Plane.Type.HORIZONTAL_UPWARD_FACING ||
                            appAnchorState != AppAnchorState.NONE) {
                        return;
                    }

                    Anchor newAnchor = fragment.getArSceneView().getSession().hostCloudAnchor(hitResult.createAnchor());
                    setCloudAnchor(newAnchor);

                    appAnchorState = AppAnchorState.HOSTING;
                    snackbarHelper.showMessage(this, "Now hosting anchor...");

                    placeObject(fragment, cloudAnchor);
                }
        );

        fragment.getArSceneView().getScene().setOnUpdateListener(this::onUpdateFrame);

        Button resolveButton = findViewById(R.id.resolve_button);
        resolveButton.setOnClickListener(view -> {
            if (cloudAnchor != null) {
                snackbarHelper.showMessageWithDismiss(getParent(), "Please clear Anchor");
                return;
            }
            ResolveDialogFragment dialog = new ResolveDialogFragment();
            dialog.setOkListener(MultiplayerActivity.this::onResolveOkPressed);
            dialog.show(getSupportFragmentManager(), "Resolve");

        });

        shootButton = findViewById(R.id.shootButton);
        shootButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                fragment.captureBitmap(new CustomArFragment.OnBitmapCapturedListener() {
                    @Override
                    public void onBitmapCaptured(Bitmap bitmap) {
                        onHitAttempted(tfMobile.detectImage(bitmap));
                    }
                }, false);
            }
        });
    }

    private void onHitAttempted(boolean isHit) {
        if (isHit) {
            Log.e("isHit", String.valueOf(isHit));
//            GameHit hit = new GameHit(getDeviceId(), hitType);
//            if (gameHitsRef != null) {
//                gameHitsRef.push().setValue(hit);
//            }
        }
    }

    @SuppressLint("HardwareIds")
    private String getDeviceId() {
        return Settings.Secure.getString(this.getContentResolver(),
                Settings.Secure.ANDROID_ID).substring(0, 5);
    }

    private void setCloudAnchor(Anchor newAnchor) {
        if (cloudAnchor != null) {
            cloudAnchor.detach();
        }

        cloudAnchor = newAnchor;
        appAnchorState = AppAnchorState.NONE;
        snackbarHelper.hide(this);
    }

    private void placeObject(ArFragment fragment, Anchor anchor) {
        ModelRenderable.builder()
                .setSource(fragment.getContext(), R.raw.andy)
                .build()
                .thenAccept(renderable -> addNodeToScene(fragment, anchor, renderable))
                .exceptionally((throwable -> {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setMessage(throwable.getMessage())
                            .setTitle("Error!");
                    AlertDialog dialog = builder.create();
                    dialog.show();
                    return null;
                }));

    }

    private void addNodeToScene(ArFragment fragment, Anchor anchor, Renderable renderable) {
        AnchorNode anchorNode = new AnchorNode(anchor);
        TransformableNode node = new TransformableNode(fragment.getTransformationSystem());
        node.setRenderable(renderable);
        node.setParent(anchorNode);
        fragment.getArSceneView().getScene().addChild(anchorNode);
        node.select();
    }

    private void onResolveOkPressed(String dialogValue) {
        int shortCode = Integer.parseInt(dialogValue);
        storageManager.getCloudAnchorID(shortCode, (cloudAnchorId) -> {
            Anchor resolvedAnchor = fragment.getArSceneView().getSession().resolveCloudAnchor(cloudAnchorId);
            setCloudAnchor(resolvedAnchor);
            placeObject(fragment, cloudAnchor);
            snackbarHelper.showMessage(this, "Now Resolving Anchor...");
            appAnchorState = AppAnchorState.RESOLVING;
        });
    }

    private void onUpdateFrame(FrameTime frameTime) {
        checkUpdatedAnchor();
    }

    private synchronized void checkUpdatedAnchor() {
        if (appAnchorState != AppAnchorState.HOSTING && appAnchorState != AppAnchorState.RESOLVING) {
            return;
        }
        Anchor.CloudAnchorState cloudState = cloudAnchor.getCloudAnchorState();
        if (appAnchorState == AppAnchorState.HOSTING) {
            if (cloudState.isError()) {
                snackbarHelper.showMessageWithDismiss(this, "Error hosting anchor.. "
                        + cloudState);
                appAnchorState = AppAnchorState.NONE;
            } else if (cloudState == Anchor.CloudAnchorState.SUCCESS) {
                storageManager.nextShortCode((shortCode) -> {
                    if (shortCode == null) {
                        snackbarHelper.showMessageWithDismiss(this, "Could not get shortCode");
                        return;
                    }
                    storageManager.storeUsingShortCode(shortCode, cloudAnchor.getCloudAnchorId());

                    snackbarHelper.showMessageWithDismiss(this, "Anchor hosted! Cloud Short Code: " +
                            shortCode);
                });

                appAnchorState = AppAnchorState.HOSTED;
            }
        } else if (appAnchorState == AppAnchorState.RESOLVING) {
            if (cloudState.isError()) {
                snackbarHelper.showMessageWithDismiss(this, "Error resolving anchor.. "
                        + cloudState);
                appAnchorState = AppAnchorState.NONE;
            } else if (cloudState == Anchor.CloudAnchorState.SUCCESS) {
                snackbarHelper.showMessageWithDismiss(this, "Anchor resolved successfully");
                appAnchorState = AppAnchorState.RESOLVED;
            }
        }
    }
}
