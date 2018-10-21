package dining.philosophers.com.kia;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.ar.core.Anchor;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.Renderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

import dining.philosophers.com.kia.GameModels.Game;
import dining.philosophers.com.kia.GameModels.GameHit;
import dining.philosophers.com.kia.GameModels.GamePlayer;
import dining.philosophers.com.kia.GameModels.GameWorldObject;
import dining.philosophers.com.kia.ml.TFMobile;
import dining.philosophers.com.kia.Utils;

import static dining.philosophers.com.kia.Utils.playReload;

public class MultiplayerActivity extends AppCompatActivity {

    private boolean isReloading;

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
    private String gameId;
    private int currentHealth = -1;

    private StorageManager storageManager;

    private TFMobile tfMobile;
    private Game game;

    private TextView tvHealth;
    private Button setMapButton;
    private ImageView crosshair;
    private ImageView shootButton;
    private TextView tvGameStatus;


    private DatabaseReference gameRef = FirebaseDatabase.getInstance().getReference("games");
    private DatabaseReference gameWorldObjectsRef;
    private DatabaseReference gamePlayersRef;
    private DatabaseReference gameHitsRef;
    private boolean isMapSet = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_multiplayer);

        fragment = (CustomArFragment) getSupportFragmentManager().findFragmentById(R.id.sceneform_fragment);
        fragment.getPlaneDiscoveryController().hide();
        setMapButton = findViewById(R.id.setMapButton);
        crosshair = findViewById(R.id.iv_crosshair);
        shootButton = findViewById(R.id.shoot_button);

        setMapButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isMapSet = true;
                setMapButton.setVisibility(View.GONE);
                crosshair.setVisibility(View.VISIBLE);
                shootButton.setVisibility(View.VISIBLE);
            }
        });

        game = new Game();
        tfMobile = new TFMobile(this);

        tvHealth = findViewById(R.id.healthTextView);
        tvGameStatus = findViewById(R.id.gameStatusTextView);

        storageManager = new StorageManager(this);

        fragment.setOnTapArPlaneListener((HitResult hitResult, Plane plane, MotionEvent motionEvent) -> {
            if (!isMapSet) {

                Anchor localAnchor = hitResult.createAnchor();

                if (appAnchorState == AppAnchorState.NONE) {

                    if (plane.getType() != Plane.Type.HORIZONTAL_UPWARD_FACING) {
                        return;
                    }

                    Anchor newAnchor = fragment.getArSceneView().getSession().hostCloudAnchor(localAnchor);
                    setCloudAnchor(newAnchor);

                    appAnchorState = AppAnchorState.HOSTING;
                    snackbarHelper.showMessage(this, "Now hosting anchor...");

                    placeObject(fragment, cloudAnchor, Uri.parse("LibertyStatue.sfb"), false);
                } else {
                    placeObject(fragment, localAnchor, Uri.parse("Pillar.sfb"), true);
                }
            }
        });

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

        shootButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isReloading) {
                    dining.philosophers.com.kia.Utils.playFireEmpty(MultiplayerActivity.this);
                    return;
                }
                isReloading = true;
                dining.philosophers.com.kia.Utils.playFireNormal(MultiplayerActivity.this);
                fragment.captureBitmap(new CustomArFragment.OnBitmapCapturedListener() {
                    @Override
                    public void onBitmapCaptured(Bitmap bitmap) {
                        onHitAttempted(tfMobile.detectImage(bitmap));
                    }
                }, false);
            }
        });
    }

    private void setupNewGame(int shortCode) {
        gameId = String.valueOf(shortCode);
        game.id = gameId;
        gameWorldObjectsRef = gameRef.child(gameId).child("objects");
        gamePlayersRef = gameRef.child(gameId).child("players");
        gameHitsRef = gameRef.child(gameId).child("hits");

        GamePlayer player = new GamePlayer();
        player.playerId = getDeviceId();
        player.health = 100;

        gameRef.child(gameId).child("looserId").setValue("");

        gamePlayersRef.child(getDeviceId()).setValue(player);
        gamePlayersRef.child(getDeviceId()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                updateGameState(dataSnapshot.getValue(GamePlayer.class));
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        gameRef.child(gameId).child("looserId").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String looserId = dataSnapshot.getValue(String.class);
                if (looserId == null || looserId.equals("") || looserId.equals(" ")) {
                    findViewById(R.id.iv_crosshair).setVisibility(View.VISIBLE);
                    tvGameStatus.setVisibility(View.GONE);
                    return;
                }

                if (!looserId.equals(getDeviceId())) {
                    crosshair.setVisibility(View.GONE);
                    tvGameStatus.setText("AAAP JEET GAYE, AUR KHELE AUR EXPERT BANNE");
                    tvGameStatus.setTextColor(Color.GREEN);
                    tvGameStatus.setVisibility(View.VISIBLE);
                } else {
                    crosshair.setVisibility(View.GONE);
                    tvGameStatus.setText("AAP HAAR GAYEE, NAYI GAME LAGAYE AUR BADAL LE");
                    tvGameStatus.setTextColor(Color.RED);
                    tvGameStatus.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void onHitAttempted(boolean isHit) {
        Log.e("isHit", String.valueOf(isHit));
        if (isHit) {
            GameHit hit = new GameHit(getDeviceId());
            if (gameHitsRef != null) {
                gameHitsRef.push().setValue(hit);
            }

        }
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            dining.philosophers.com.kia.Utils.playReload(this);
            isReloading = false;
        }, 1000);
    }

    private void updateGameState(GamePlayer player) {
        if (currentHealth != -1) {
            int damage = currentHealth - player.health;
        }
        currentHealth = player.health;
        tvHealth.setText(String.valueOf(player.health));
//        healthProgress.setProgress(currentHealth);

        if (currentHealth <= 30) {
            setHealthProgressColor(Color.RED);
        }

        if (currentHealth > 30 && currentHealth <= 70) {
            setHealthProgressColor(Color.YELLOW);
        }

        if (currentHealth > 70) {
            setHealthProgressColor(Color.GREEN);
        }
    }

    public void setHealthProgressColor(int color) {
        tvHealth.setTextColor(color);
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

    private void placeObject(ArFragment fragment, Anchor anchor, Uri model, boolean doSync) {
        ModelRenderable.builder()
                .setSource(fragment.getContext(), model)
                .build()
                .thenAccept(renderable -> addNodeToScene(fragment, anchor, renderable, doSync))
                .exceptionally((throwable -> {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setMessage(throwable.getMessage())
                            .setTitle("Error!");
                    AlertDialog dialog = builder.create();
                    dialog.show();
                    return null;
                }));
    }

    private void syncNewObject(AnchorNode anchorNode) {
        Vector3 position = anchorNode.getWorldPosition();
        Quaternion rotation = anchorNode.getWorldRotation();
        DatabaseReference newObjectRef = gameWorldObjectsRef.push();
        GameWorldObject worldObject = new GameWorldObject(position, rotation, newObjectRef.getKey(), getDeviceId());
        game.gameWorldObject.add(worldObject);
        newObjectRef.setValue(worldObject);
    }

    private void addNodeToScene(ArFragment fragment, Anchor anchor, Renderable renderable, boolean doSync) {
        AnchorNode anchorNode = new AnchorNode(anchor);
        Node node = new Node();
        renderable.setShadowCaster(false);
        node.setRenderable(renderable);
        node.setParent(anchorNode);
        fragment.getArSceneView().getScene().addChild(anchorNode);
        if (doSync) {
            syncNewObject(anchorNode);
        }
    }

    private void addHealthSyncing() {
        gameHitsRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                try {
                    GameHit hit = dataSnapshot.getValue(GameHit.class);
                    if (!hit.hitBy.equals(getDeviceId())) {
                        int newHealth = currentHealth - 10;
                        if (newHealth < 0) {
                            newHealth = 0;
                        }
                        gamePlayersRef.child(getDeviceId()).child("health").setValue((newHealth));
                        if (newHealth == 0) {
                            gameRef.child(gameId).child("looserId").setValue(getDeviceId());
                        }
                    }
                } catch (Exception e) {
                    Log.e("MultiplayerAcitivity", e.getMessage());
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }

    private void addChildSyncing() {
        gameWorldObjectsRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                try {
                    GameWorldObject worldObject = dataSnapshot.getValue(GameWorldObject.class);
                    if (worldObject.addedByDeviceId.equals(getDeviceId())) {
                        return;
                    }
                    Session session = fragment.getArSceneView().getSession();
                    Anchor anchor = session.createAnchor(new Pose(getArray(worldObject.position), getArray(worldObject.rotation)));
                    placeObject(fragment, anchor, Uri.parse("Pillar.sfb"), false);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
        gameWorldObjectsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    try {
                        GameWorldObject worldObject = snapshot.getValue(GameWorldObject.class);
                        Session session = fragment.getArSceneView().getSession();
                        Anchor anchor = session.createAnchor(new Pose(getArray(worldObject.position), getArray(worldObject.rotation)));
                        placeObject(fragment, anchor, Uri.parse("Pillar.sfb"), false);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private float[] getArray(ArrayList<Float> list) {
        float[] floatArray = new float[list.size()];
        int i = 0;

        for (Float f : list) {
            floatArray[i++] = (f != null ? f : Float.NaN);
        }

        return floatArray;
    }

    private void onResolveOkPressed(String dialogValue) {
        int shortCode = Integer.parseInt(dialogValue);
        setupNewGame(shortCode);
        storageManager.getCloudAnchorID(shortCode, (cloudAnchorId) -> {
            Anchor resolvedAnchor = fragment.getArSceneView().getSession().resolveCloudAnchor(cloudAnchorId);
            setCloudAnchor(resolvedAnchor);
            placeObject(fragment, cloudAnchor, Uri.parse("LibertyStatue.sfb"), false);
            snackbarHelper.showMessage(this, "Now Resolving Anchor...");
            appAnchorState = AppAnchorState.RESOLVING;
            addChildSyncing();
            addHealthSyncing();
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
                    setupNewGame(shortCode);

                    snackbarHelper.showMessageWithDismiss(this, "Anchor hosted! Cloud Short Code: " +
                            shortCode);

                    addChildSyncing();
                    addHealthSyncing();
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
