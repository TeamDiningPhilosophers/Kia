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

import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class PlayAreaActivity extends AppCompatActivity {

    private ModelRenderable mAndyRederable;
    private ArFragment mPlayAreaFragment;
    private Session mSession;
    private ScheduledExecutorService schedule= Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture schedulerHandler;
    private Anchor anchor;
    private AnchorNode anchorNode;
    private Node andy;
    private int hit=0;
    private int miss=0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play_area);

        mPlayAreaFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.playAreaFragment);
        Log.d("Play Area",mPlayAreaFragment.getArSceneView().getScene().toString());
        buildAndyRenderable();

        new Handler().postDelayed(this::generateAndy, 10000);
        // scheduling the task at fixed rate
        schedulerHandler=schedule.scheduleAtFixedRate(() -> runOnUiThread(() -> placeAndyAtRandom()),3,3, TimeUnit.SECONDS);

    }

    @Override
    protected void onPause() {
        super.onPause();
        schedulerHandler.cancel(true);
    }

    public void generateAndy() {
        anchor = null;
        mSession = mPlayAreaFragment.getArSceneView().getSession();
        for(Plane plane: mSession.getAllTrackables(Plane.class)){
            if(plane.getType()==Plane.Type.HORIZONTAL_UPWARD_FACING
                    && plane.getTrackingState()== TrackingState.TRACKING){
                anchor=plane.createAnchor(plane.getCenterPose());
                Log.d("Plane anchor",plane.toString());
                break;
            }
        }

        anchorNode = new AnchorNode(anchor);
        anchorNode.setParent(mPlayAreaFragment.getArSceneView().getScene());
        andy = new Node();
        andy.setParent(anchorNode);
        andy.setRenderable(mAndyRederable);
        andy.setLocalPosition(new Vector3(0f,0,0f));
        andy.setOnTapListener((hitTestResult ,motionEvent) -> {
            andy.setEnabled(false);
            hit++;
        });
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
    public void placeAndyAtRandom(){
        if(andy!=null){
            if(andy.isEnabled()==true){
                miss++;
            }
            andy.setEnabled(true);
            //mPlayAreaFragment.getArSceneView().getX
            andy.setLocalPosition(new Vector3(randFloat(-0.3f,0.8f),0,randFloat(-0.8f,0.8f)));
        }
    }
    public static float randFloat(float min, float max) {
        Random rand = new Random();
        float result = rand.nextFloat() * (max - min) + min;
        return result;
    }

}
