
package com.example.convertoapp;


import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.itextpdf.text.Document;
import com.itextpdf.text.Image;
import com.itextpdf.text.pdf.PdfWriter;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Img_to_pdf extends AppCompatActivity {

    private static final int REQUEST_PERMISSION = 1;

    private Button btnSelectImages;
    private Button btnConvertToPdf;
    private GridView gridView;
    private ImageAdapter imageAdapter;

    private List<Uri> selectedImages = new ArrayList<>();

    private ActivityResultLauncher<Intent> imagePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_img_to_pdf);

        btnSelectImages = findViewById(R.id.btnSelectImages);
        btnConvertToPdf = findViewById(R.id.btnConvertToPdf);
        gridView = findViewById(R.id.gridView);

        imageAdapter = new ImageAdapter();
        gridView.setAdapter(imageAdapter);

        btnSelectImages.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkPermissionsAndOpenGallery();
            }
        });

        btnConvertToPdf.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                convertToPdf();
            }
        });

        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Uri imageUri = selectedImages.get(position);
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(imageUri, "image/*");
                startActivity(intent);
            }
        });

        imagePickerLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                this::handleImagePickerResult);
    }

    private void checkPermissionsAndOpenGallery() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    REQUEST_PERMISSION);
        } else {
            openGallery();
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        imagePickerLauncher.launch(Intent.createChooser(intent, "Select Images"));
    }

    private void convertToPdf() {
        if (selectedImages.isEmpty()) {
            Toast.makeText(this, "No images selected.", Toast.LENGTH_SHORT).show();
            return;
        }

        Document document = new Document();

        String pdfFilePath = Environment.getExternalStorageDirectory().getPath() + "/converted_images.pdf";
        File pdfFile = new File(pdfFilePath);
        try {
            PdfWriter.getInstance(document, new FileOutputStream(pdfFile));
            document.open();

            for (Uri imageUri : selectedImages) {
                try {
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                    Image image = Image.getInstance(bitmapToByteArray(bitmap));
                    document.add(image);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            document.close();

            Toast.makeText(this, "PDF created successfully.", Toast.LENGTH_SHORT).show();
            openPdfFile(pdfFile);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to create PDF.", Toast.LENGTH_SHORT).show();
        }
    }

    private byte[] bitmapToByteArray(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        return stream.toByteArray();
    }

    private void handleImagePickerResult(ActivityResult result) {
        if (result.getResultCode() == RESULT_OK) {
            Intent data = result.getData();
            if (data.getClipData() != null) {
                int count = data.getClipData().getItemCount();
                for (int i = 0; i < count; i++) {
                    Uri imageUri = data.getClipData().getItemAt(i).getUri();
                    selectedImages.add(imageUri);
                    imageAdapter.notifyDataSetChanged();
                }
            } else if (data.getData() != null) {
                Uri imageUri = data.getData();
                selectedImages.add(imageUri);
                imageAdapter.notifyDataSetChanged();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openGallery();
            } else {
                Toast.makeText(this, "Permission denied.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private class ImageAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return selectedImages.size();
        }

        @Override
        public Object getItem(int position) {
            return selectedImages.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ImageView imageView;
            if (convertView == null) {
                imageView = new ImageView(Img_to_pdf.this);
                imageView.setLayoutParams(new GridView.LayoutParams(250, 250));
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            } else {
                imageView = (ImageView) convertView;
            }
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(Img_to_pdf.this.getContentResolver(), selectedImages.get(position));
                imageView.setImageBitmap(bitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return imageView;
        }
    }

    private void openPdfFile(File file) {
        Uri uri = Uri.fromFile(file);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "application/pdf");
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        // Verify that there is a PDF viewer app available
        PackageManager packageManager = getPackageManager();
        List<ResolveInfo> activities = packageManager.queryIntentActivities(intent, 0);
        boolean isIntentSafe = activities.size() > 0;

        if (isIntentSafe) {
            Intent chooser = Intent.createChooser(intent, "Open PDF with");
            startActivity(chooser);
        } else {
            Toast.makeText(this, "No PDF viewer app found. Please install a PDF viewer app to open the PDF.", Toast.LENGTH_SHORT).show();
            // Optionally, provide alternative actions for the user, such as opening the file using a file manager app
        }
    }

}
