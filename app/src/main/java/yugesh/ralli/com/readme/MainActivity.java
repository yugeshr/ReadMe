package yugesh.ralli.com.readme;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.FirebaseApp;
import com.google.firebase.ml.naturallanguage.FirebaseNaturalLanguage;
import com.google.firebase.ml.naturallanguage.languageid.FirebaseLanguageIdentification;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;
import com.google.firebase.ml.vision.text.RecognizedLanguage;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
public class MainActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    FloatingActionButton btn_photo;
    TextView textView;

    private static final int REQUEST_TAKE_PHOTO = 100;
    private final int PERMISSION_REQUEST_CAMERA = 1;
    private final int PERMISSION_REQUEST_STORAGE = 2;
    private final int TTS_CHECK_CODE = 3;

    private String mCurrentPhotoPath, resultText;
    private Bitmap mSelectedImage;
    private TextToSpeech textToSpeech;

    int PERMISSION_ALL = 1;
    String[] PERMISSIONS = {
            android.Manifest.permission.READ_CONTACTS,
            android.Manifest.permission.WRITE_CONTACTS,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
            android.Manifest.permission.READ_SMS,
            android.Manifest.permission.CAMERA
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if(!hasPermissions(this, PERMISSIONS)){
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL);
        }

        //checkTTS
        checkTTS();

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        btn_photo = findViewById(R.id.actionButton);
        textView = findViewById(R.id.textView);

        btn_photo.setOnClickListener(view -> {
            cropImage();
        });

        textView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                ClipboardManager clipboard=(ClipboardManager)getApplicationContext().getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clipData = ClipData.newPlainText("copy",textView.getText().toString());
                clipboard.setPrimaryClip(clipData);
                Toast.makeText(getApplicationContext(),"Copied to clipboard",Toast.LENGTH_SHORT).show();
                return false;
            }
        });
    }

    public static boolean hasPermissions(Context context, String... permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_CAMERA:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //Toast.makeText(getApplicationContext(), "Permission Granted, Now you can access Camera", Toast.LENGTH_SHORT).show();
                } else {
                    //Toast.makeText(getApplicationContext(), "Permission Denied, You cannot access Camera.", Toast.LENGTH_SHORT).show();
                }
            case PERMISSION_REQUEST_STORAGE:
                if (grantResults.length == PackageManager.PERMISSION_GRANTED){
                    //Toast.makeText(getApplicationContext(), "Permission Granted, Now you can access Storage", Toast.LENGTH_SHORT).show();
                } else {
                    //Toast.makeText(getApplicationContext(), "Permission Denied, Now you canot access Storage", Toast.LENGTH_SHORT).show();
                }
        }
    }

    private void cropImage() {
        // start picker to get image for cropping and then use the image in cropping activity
        CropImage.activity()
                .setGuidelines(CropImageView.Guidelines.ON)
                .start(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data){
        if(requestCode == TTS_CHECK_CODE){
            if(resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS){
                // success, create the TTS instance
                textToSpeech = new TextToSpeech(this,this);
            }else {
                // missing data, install it
                Intent installIntent = new Intent();
                installIntent.setAction(
                        TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
                startActivity(installIntent);
            }
        }
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                Uri resultUri = result.getUri();
                File imageFile = new File(resultUri.getPath());
                Log.d("TAG",resultUri.getPath());
                Bitmap imageBitmap = BitmapFactory.decodeFile(imageFile.getPath());
                mSelectedImage = imageBitmap; //rotateImageIfRequired(imageBitmap,imageFile);
                recogniseText();
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
            }
        }

    }

    private void recogniseText() {
        FirebaseApp.initializeApp(this);
        FirebaseVisionImage firebaseVisionImage = FirebaseVisionImage.fromBitmap(mSelectedImage);
        FirebaseVisionTextRecognizer firebaseVisionTextRecognizer = FirebaseVision.getInstance()
                .getOnDeviceTextRecognizer();
        // Task completed successfully
// ...
        Task<FirebaseVisionText> firebaseVisionTextTask =
                firebaseVisionTextRecognizer.processImage(firebaseVisionImage)
                        .addOnSuccessListener(this::extractTextFromResult)
                        .addOnFailureListener(e -> {
                                    // Task failed with an exception
                                    // ...
                                });
    }

    private void extractTextFromResult(FirebaseVisionText firebaseVisionText) {
        resultText = firebaseVisionText.getText();
        Log.d("TAG",resultText);
        textView.setText(resultText);
        saveTextAsFile(resultText);
        identifyLanguage(resultText);
        for (FirebaseVisionText.TextBlock block: firebaseVisionText.getTextBlocks()) {
            String blockText = block.getText();
            Float blockConfidence = block.getConfidence();
            List<RecognizedLanguage> blockLanguages = block.getRecognizedLanguages();
            Point[] blockCornerPoints = block.getCornerPoints();
            Rect blockFrame = block.getBoundingBox();
            for (FirebaseVisionText.Line line: block.getLines()) {
                String lineText = line.getText();
                Float lineConfidence = line.getConfidence();
                List<RecognizedLanguage> lineLanguages = line.getRecognizedLanguages();
                Point[] lineCornerPoints = line.getCornerPoints();
                Rect lineFrame = line.getBoundingBox();
                for (FirebaseVisionText.Element element: line.getElements()) {
                    String elementText = element.getText();
                    Float elementConfidence = element.getConfidence();
                    List<RecognizedLanguage> elementLanguages = element.getRecognizedLanguages();
                    Point[] elementCornerPoints = element.getCornerPoints();
                    Rect elementFrame = element.getBoundingBox();
                }
            }
        }
    }

    private String createFileName(){
        // Create an text file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        return "TEXT_" + timeStamp;
    }

    private void saveTextAsFile(String content){
        String fileName = createFileName() + ".txt";
        Log.d("TAG",fileName);
        File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/ReadMe/Text files",fileName);

        FileOutputStream fileOutputStream = null;
        try {
            fileOutputStream = new FileOutputStream(file);
            fileOutputStream.write(content.getBytes());
            fileOutputStream.close();
            Log.d("TAG","Saved!");

        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Log.d("TAG","File not found!");
        } catch (IOException e){
            e.printStackTrace();
            Log.d("TAG","Error saving!");
        }

    }

    private void identifyLanguage(String resultText) {
        FirebaseLanguageIdentification languageIdentifier =
                FirebaseNaturalLanguage.getInstance().getLanguageIdentification();
        languageIdentifier.identifyLanguage(resultText)
                .addOnSuccessListener(
                        languageCode -> {
                            if (languageCode != "und") {
                                Log.i("TAG", "Language: " + languageCode);
                            } else {
                                Log.i("TAG", "Can't identify language.");
                            }
                        })
                .addOnFailureListener(
                        e -> {
                            // Model couldn’t be loaded or other internal error.
                            // ...
                        });
    }

    private void checkTTS(){
        Intent checkIntent = new Intent();
        checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
        startActivityForResult(checkIntent, TTS_CHECK_CODE);
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            if (textToSpeech != null) {
                int result = textToSpeech.setLanguage(Locale.US);
                if (result == TextToSpeech.LANG_MISSING_DATA ||
                        result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.d("TAG", "TTS language is not supported ");
                } else {
                    textToSpeech.setSpeechRate((float) 0.7);
                    Log.d("TAG", "TTS is ready");
                }
            }
        } else {
            Log.d("TAG", "TTS initialization failed");
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.option_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id){
            case R.id.action_tts : {
                textToSpeech.speak(resultText,TextToSpeech.QUEUE_ADD,null);
                break;
            }
            case R.id.action_stop : {
                textToSpeech.stop();
                break;
            }
        }
        return super.onOptionsItemSelected(item);
    }
}
