package com.haxon.imagepreviewer;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Log;
import android.widget.Toast;

import com.haxon.imagepreviewer.databinding.ActivityMainBinding;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    private static final int PICK_IMAGE = 910;
    private static final int PICK_CAMERA = 469;
    public static String imageFileName, currentPhotoPath;
    private Uri contentUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (getIntent().getBooleanExtra("edit", false)){
            Uri image = getIntent().getParcelableExtra("image");
            binding.imagePreview.setImageURI(image);
        }else {
            binding.imagePreview.setImageResource(R.drawable.place_holder);
        }

        // Button to open the front camera
        binding.btnCamera.setOnClickListener(view -> {
            if (!checkCameraPermission()) {
                requestCameraPermission();
            } else {
                cameraCapture();
            }
        });

        // Button to open the gallery
        binding.btnGallery.setOnClickListener(view -> {
            if (!checkStoragePermission()) {
                requestStoragePermission();
            } else {
                pickImage();
            }
        });
    }

     /*This is picked from Android Documentation
     * https://developer.android.com/training/camera/photobasics
     * */
    private void cameraCapture() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
                ex.getLocalizedMessage();
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.haxon.imagepreviewer.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                takePictureIntent.putExtra("android.intent.extras.CAMERA_FACING", 1);
                startActivityForResult(takePictureIntent, PICK_CAMERA);
            }
        }
    }

    /*This is picked from Android Documentation
     * https://developer.android.com/training/camera/photobasics
     * */
    private File createImageFile() throws IOException {
        // Create an image file name
        @SuppressLint("SimpleDateFormat")
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        imageFileName = "image_" + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();
        Log.e("photoPath", currentPhotoPath);
        return image;
    }

    /*This is picked from Android Documentation
     * https://developer.android.com/training/camera/photobasics
     * */
    private void galleryAddPic() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(currentPhotoPath);
        contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);
    }

    private void pickImage() {

        Intent pickIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickIntent.setType("image/*");

        startActivityForResult(pickIntent, PICK_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == PICK_IMAGE) {
                if (data != null) {
                    Uri photo = data.getData();
                    imageFileName = getFileName(photo);
                    Uri new_photo_uri = Uri.parse("file:///storage/emulated/0/Pictures/"+imageFileName);
                    Log.e("FILE_NAME", new_photo_uri +"");
                    Intent intent = new Intent(this, EditActivity.class);
                    intent.putExtra("image", new_photo_uri);
                    startActivity(intent);

                }
            } else if (requestCode == PICK_CAMERA) {
                galleryAddPic();
                Intent intent = new Intent(this, EditActivity.class);
                intent.putExtra("image", contentUri);
                startActivity(intent);
            } else {
                Toast.makeText(this, "No Photo selected", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "No Photo selected", Toast.LENGTH_SHORT).show();
        }
    }

    // This function returns the file name from the URI
    @SuppressLint("Range")
    public String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    // Request function for the storage permission
    private void requestStoragePermission() {
        requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PICK_IMAGE);
    }

    // Request function for both Camera and Storage permission
    private void requestCameraPermission() {
        requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, PICK_CAMERA);
    }

    // Checks the storage permission
    private boolean checkStoragePermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;
    }

    // Checks the camera permission
    private boolean checkCameraPermission() {
        boolean permission_camera = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
        boolean permission_storage = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;

        return permission_camera && permission_storage;
    }
}