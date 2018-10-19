package dining.philosophers.com.kia;

import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;

import com.google.ar.core.Anchor;
import com.google.ar.core.Plane;
import com.google.ar.core.Session;
import com.google.ar.core.TrackingState;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.ux.ArFragment;

public class PlayAreaActivity extends AppCompatActivity {

    private ModelRenderable mAndyRederable;
    private ArFragment mPlayAreaFragment;

    private Session mSession;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play_area);

        mPlayAreaFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.playAreaFragment);

        buildAndyRenderable();

        new Handler().postDelayed(this::generateAndyAtRandomPosition, 10000);
    }

    public void generateAndyAtRandomPosition() {
        Anchor anchor = null;
        mSession = mPlayAreaFragment.getArSceneView().getSession();
        for(Plane plane: mSession.getAllTrackables(Plane.class)){
            if(plane.getType()==Plane.Type.HORIZONTAL_UPWARD_FACING
                    && plane.getTrackingState()== TrackingState.TRACKING){
                anchor=plane.createAnchor(plane.getCenterPose());
                Log.d("Plane anchor",plane.toString());
                break;
            }
        }

        AnchorNode anchorNode = new AnchorNode(anchor);
        anchorNode.setParent(mPlayAreaFragment.getArSceneView().getScene());
        Node andy = new Node();
        andy.setParent(anchorNode);
        andy.setRenderable(mAndyRederable);
        andy.setLocalPosition(new Vector3(1f,0,0.5f));
        andy.setOnTapListener((hitTestResult ,motionEvent) -> andy.setEnabled(false));
    }

    public void buildAndyRenderable() {
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
