package dining.philosophers.com.kia;

import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Toast;

import com.google.ar.core.Anchor;
import com.google.ar.core.Frame;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;

public class PlayAreaActivity extends AppCompatActivity {

    private ModelRenderable mAndyRederable;
    private ArFragment mPlayAreaFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play_area);

        mPlayAreaFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.playAreaFragment);

        buildAndyRederable();

        new Handler().postDelayed(this::generateAndyAtRandomPosition, 10000);
    }



    public void generateAndyAtRandomPosition() {

        Anchor anchor = null;

        try {
            Frame arFrame = mPlayAreaFragment.getArSceneView().getArFrame();
            Pose pose = arFrame.getCamera().getPose();

            anchor = new Session(this).createAnchor(pose);
        } catch (Exception e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }

        AnchorNode anchorNode = new AnchorNode(anchor);
        anchorNode.setParent(mPlayAreaFragment.getArSceneView().getScene());

        TransformableNode andy = new TransformableNode(mPlayAreaFragment.getTransformationSystem());
        andy.setParent(anchorNode);
        andy.setRenderable(mAndyRederable);
        andy.select();
    }

    public void buildAndyRederable() {
        ModelRenderable.builder()
                .setSource(this, R.raw.andy)
                .build()
                .thenAccept(modelRenderable -> mAndyRederable = modelRenderable)
                .exceptionally(
                        throwable -> {
                            Toast toast =
                                    Toast.makeText(this, "Unable to load andy renderable", Toast.LENGTH_LONG);
                            toast.setGravity(Gravity.CENTER, 0, 0);
                            toast.show();
                            return null;
                        });
    }
}
