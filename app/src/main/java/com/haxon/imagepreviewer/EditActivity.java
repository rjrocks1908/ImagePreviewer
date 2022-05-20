package com.haxon.imagepreviewer;


import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.animation.RotateAnimation;
import android.widget.Toast;

import com.haxon.imagepreviewer.databinding.ActivityEditBinding;
import com.yalantis.ucrop.UCrop;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class EditActivity extends AppCompatActivity {
    private static final int PIC_CROP = 100;
    private ActivityEditBinding binding;
    private boolean isRotate;
    private float mCurrRotation = 0, fromRotation, toRotation;
    private Bitmap croppedBitmap = null, cropThenRotateBitmap = null, rotateBitmap = null, originalBitmap, rotateThenCropBitmap = null;
    private Uri croppedUri, picUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEditBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        picUri = getIntent().getParcelableExtra("image");
        croppedUri = picUri;
        binding.imagePreview.setImageURI(picUri);
        BitmapDrawable drawable = (BitmapDrawable) binding.imagePreview.getDrawable();
        originalBitmap = drawable.getBitmap();
        Log.e("PIC_URI", picUri + "");

        binding.btnRotate.setOnClickListener(view -> rotate());
        binding.btnUndo.setOnClickListener(view -> undo());
        binding.btnCrop.setOnClickListener(view -> crop());
        binding.btnSave.setOnClickListener(view -> {
            try {
                save();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public void rotate() {
        isRotate = true;
        mCurrRotation %= 360;
        Matrix matrix = new Matrix();


        Log.e("GET_ROTATION", binding.imagePreview.getRotation()+"");
        fromRotation = mCurrRotation;
        toRotation = mCurrRotation += 90;

        final RotateAnimation rotateAnimation = new RotateAnimation(
                fromRotation,
                toRotation,
                binding.imagePreview.getWidth() / 2f,
                binding.imagePreview.getHeight() / 2f);

        rotateAnimation.setDuration(500);
        rotateAnimation.setFillAfter(true);


        matrix.setRotate(toRotation);
        Log.e("To Rotation", toRotation + " TO ROTATION");
        Log.e("From Rotation", fromRotation + " FROM ROTATION");
        if (croppedBitmap != null) {
            cropThenRotateBitmap = Bitmap.createBitmap(croppedBitmap, 0, 0, croppedBitmap.getWidth(), croppedBitmap.getHeight(), matrix, true);
        } else {
            rotateBitmap = Bitmap.createBitmap(originalBitmap, 0, 0, originalBitmap.getWidth(), originalBitmap.getHeight(), matrix, true);
        }


        binding.imagePreview.startAnimation(rotateAnimation);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null) {
            if (requestCode == PIC_CROP) {
                //get the returned data
                Bundle extras = data.getExtras();
                //get the cropped bitmap
                Bitmap thePic = extras.getParcelable("data");
                //display the returned cropped image
                binding.imagePreview.setImageBitmap(thePic);
            } else if (requestCode == 101) {
                final Uri resultUri = UCrop.getOutput(data);
                Log.e("CROPPED", resultUri + "");
                try {
                    croppedBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), resultUri);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                binding.imagePreview.setImageBitmap(croppedBitmap);
                if (isRotate) {
                    rotateThenCropBitmap = croppedBitmap;
                    binding.imagePreview.setImageBitmap(rotateThenCropBitmap);
                }
            }
        }
    }

    public void crop() {

        if (rotateBitmap != null) {
            File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
            try {
                @SuppressLint("SimpleDateFormat")
                String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                String imageFileName = "image_" + timeStamp + "_";
                File image = File.createTempFile(
                        imageFileName,  /* prefix */
                        ".jpg",         /* suffix */
                        storageDir      /* directory */
                );
                FileOutputStream fileOutputStream = new FileOutputStream(image);
                rotateBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream);
                fileOutputStream.close();
                Log.e("CROP_IMAGE_PATH", image.getAbsolutePath() + "");
                croppedUri = Uri.fromFile(image);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        Uri uri = croppedUri;
        UCrop.of(croppedUri, uri)
                .start(this, 101);
    }

    public void undo() {
        Matrix matrix = new Matrix();

        toRotation = 90;

        final RotateAnimation rotateAnimation = new RotateAnimation(
                fromRotation,
                0,
                binding.imagePreview.getWidth() / 2f,
                binding.imagePreview.getHeight() / 2f
        );

        rotateAnimation.setDuration(500);
        rotateAnimation.setFillAfter(true);


        matrix.setRotate(toRotation);
        System.out.println(toRotation + "TO ROTATION");
        System.out.println(fromRotation + "FROM ROTATION");
        if (croppedBitmap != null) {
            cropThenRotateBitmap = Bitmap.createBitmap(croppedBitmap, 0, 0, croppedBitmap.getWidth(), croppedBitmap.getHeight(), matrix, true);
        } else {
            rotateBitmap = Bitmap.createBitmap(originalBitmap, 0, 0, originalBitmap.getWidth(), originalBitmap.getHeight(), matrix, true);
        }

        binding.imagePreview.startAnimation(rotateAnimation);
        fromRotation = mCurrRotation = 0;
        makeBitmapNull();
    }

    private void makeBitmapNull() {
        binding.imagePreview.setImageBitmap(originalBitmap);
        croppedBitmap = null;
        rotateThenCropBitmap = null;
        rotateBitmap = null;
        cropThenRotateBitmap = null;
        croppedUri = picUri;
    }

    public void save() throws IOException {

        @SuppressLint("SimpleDateFormat")
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "image_" + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        FileOutputStream imageOutStream = null;
        try {

            imageOutStream = new FileOutputStream(image);
            if (cropThenRotateBitmap != null) {
                if (!cropThenRotateBitmap.compress(Bitmap.CompressFormat.JPEG, 100, imageOutStream)) {
                    throw new IOException("Failed to compress bitmap");
                }
            } else if (rotateThenCropBitmap != null) {
                if (!rotateThenCropBitmap.compress(Bitmap.CompressFormat.JPEG, 100, imageOutStream)) {
                    throw new IOException("Failed to compress bitmap");
                }
            } else if (croppedBitmap != null) {
                if (!croppedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, imageOutStream)) {
                    throw new IOException("Failed to compress bitmap");
                }
            } else if (rotateBitmap != null) {
                if (!rotateBitmap.compress(Bitmap.CompressFormat.JPEG, 100, imageOutStream)) {
                    throw new IOException("Failed to compress bitmap");
                }
            } else {
                if (!originalBitmap.compress(Bitmap.CompressFormat.JPEG, 100, imageOutStream)) {
                    throw new IOException("Failed to compress bitmap");
                }
            }

            Toast.makeText(this, "Image Saved", Toast.LENGTH_SHORT).show();

        } finally {
            if (imageOutStream != null) {
                imageOutStream.close();

                Uri contentUri = Uri.fromFile(image);
                Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                mediaScanIntent.setData(contentUri);
                this.sendBroadcast(mediaScanIntent);

                Intent intent = new Intent(this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.putExtra("image", contentUri);
                intent.putExtra("edit", true);
                startActivity(intent);
                finish();
            }
        }

    }

}