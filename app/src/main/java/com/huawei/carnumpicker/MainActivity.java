package com.huawei.carnumpicker;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.huaweicloud.sdk.core.auth.BasicCredentials;
import com.huaweicloud.sdk.core.auth.ICredential;
import com.huaweicloud.sdk.core.exception.ConnectionException;
import com.huaweicloud.sdk.core.exception.RequestTimeoutException;
import com.huaweicloud.sdk.core.exception.ServiceResponseException;
import com.huaweicloud.sdk.ocr.v1.OcrClient;
import com.huaweicloud.sdk.ocr.v1.model.GeneralTableRequestBody;
import com.huaweicloud.sdk.ocr.v1.model.GeneralTableResult;
import com.huaweicloud.sdk.ocr.v1.model.GeneralTableWordsBlockList;
import com.huaweicloud.sdk.ocr.v1.model.GeneralTextRequestBody;
import com.huaweicloud.sdk.ocr.v1.model.LicensePlateRequestBody;
import com.huaweicloud.sdk.ocr.v1.model.RecognizeGeneralTableRequest;
import com.huaweicloud.sdk.ocr.v1.model.RecognizeGeneralTableResponse;
import com.huaweicloud.sdk.ocr.v1.model.RecognizeGeneralTextRequest;
import com.huaweicloud.sdk.ocr.v1.model.RecognizeGeneralTextResponse;
import com.huaweicloud.sdk.ocr.v1.model.RecognizeLicensePlateRequest;
import com.huaweicloud.sdk.ocr.v1.model.RecognizeLicensePlateResponse;
import com.huaweicloud.sdk.ocr.v1.model.WordsRegionList;
import com.huaweicloud.sdk.ocr.v1.region.OcrRegion;
import com.socks.library.KLog;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_CODE_TAKE_PICTURE = 0x11;
    private static final int CROP_PHOTO = 0x12;
    private static final int REQUEST_PERMISSION_CODE = 0x13;
    private static final String TAG = MainActivity.class.getSimpleName();

    private TextView mResultTv;
    private ImageView mImageIv;
    private String filePath;
    private Uri fileUri;
    private Bitmap bitmap;
    private String[] parsedResultData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mResultTv = (TextView) findViewById(R.id.rec_result);
        mImageIv = (ImageView) findViewById(R.id.image_iv);

        findViewById(R.id.capture_btn).setOnClickListener(onClickListener);
        findViewById(R.id.parse_btn).setOnClickListener(onClickListener);
        findViewById(R.id.filter_btn).setOnClickListener(onClickListener);
    }

    private final View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int vId = v.getId();
            if (vId == R.id.capture_btn) {
                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_PERMISSION_CODE);
                    return;
                }
                takePhoto();
            } else if (vId == R.id.parse_btn) {
                if (bitmap == null) {
                    Toast.makeText(MainActivity.this, "先拍照", Toast.LENGTH_SHORT).show();
                    return;
                }
                fromTable(bitmap);
            } else if (vId == R.id.filter_btn) {
                if (parsedResultData == null) {
                    Toast.makeText(MainActivity.this, "无解析数据", Toast.LENGTH_SHORT).show();
                    return;
                }
                Intent intent = new Intent(MainActivity.this, CarNumListActivity.class);
                intent.putExtra("data", parsedResultData);
                startActivity(intent);
            }
        }
    };

    private void takePhoto() {
        Intent openCameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        filePath = Environment.getExternalStoragePublicDirectory("").getPath() + File.separator + "image_" + System.currentTimeMillis() + ".jpg";
        fileUri = FileProvider.getUriForFile(MainActivity.this, getPackageName() + ".fileprovider", new File(filePath));

        openCameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);
        startActivityForResult(openCameraIntent, REQUEST_CODE_TAKE_PICTURE);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                Toast.makeText(this, "storage permission deny", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            takePhoto();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == REQUEST_CODE_TAKE_PICTURE) {
            try {
                bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(fileUri));
                mImageIv.setImageBitmap(bitmap);

                //拍照完了，马上解析
                fromTable(bitmap);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    private OcrClient base() {
        String ak = "W51YHXVNYDQQWR47XDOB";
        String sk = "5ZB9QoujRjRwlnupL0TYDNCMb53qnfLRaWLF196L";
        ICredential auth = new BasicCredentials()
                .withAk(ak)
                .withSk(sk);
        return OcrClient.newBuilder()
                .withCredential(auth)
                .withRegion(OcrRegion.valueOf("cn-north-4"))
                .build();
    }

    /**
     * RecognizeLicensePlate
     * 车牌识别
     */
    private void fromBase64() {
        RecognizeLicensePlateRequest request = new RecognizeLicensePlateRequest();
        LicensePlateRequestBody body = new LicensePlateRequestBody();
//        body.withUrl("https://ss2.meipian.me/users/73714316/2387be42cc8748b99ab17f195034fff2.jpg");
        body.withUrl("https://p3.itc.cn/images01/20210721/19df6508fcfb40789182a058e10175ac.jpeg");
//        body.withImage(image_base64);
        request.withBody(body);
        try {
            RecognizeLicensePlateResponse response = base().recognizeLicensePlate(request);
            String result = response.toString();
            System.out.println("识别结果：" + result);
            KLog.e();
            showResultOnView(result);
        } catch (ConnectionException | RequestTimeoutException e) {
            e.printStackTrace();
        } catch (ServiceResponseException e) {
            e.printStackTrace();
            System.out.println(e.getHttpStatusCode());
            System.out.println(e.getErrorCode());
            System.out.println(e.getErrorMsg());
        }
    }

    /**
     * RecognizeGeneralText
     * 通用文字识别
     */
    private void fromUrl() {
        RecognizeGeneralTextRequest request = new RecognizeGeneralTextRequest();
        GeneralTextRequestBody body = new GeneralTextRequestBody();
        body.withQuickMode(true);
        body.withDetectDirection(true);
        body.withUrl("https://ss2.meipian.me/users/73714316/2387be42cc8748b99ab17f195034fff2.jpg");
        request.withBody(body);
        try {
            RecognizeGeneralTextResponse response = base().recognizeGeneralText(request);
            System.out.println(response.toString());
        } catch (ConnectionException | RequestTimeoutException e) {
            e.printStackTrace();
        } catch (ServiceResponseException e) {
            e.printStackTrace();
            System.out.println(e.getHttpStatusCode());
            System.out.println(e.getErrorCode());
            System.out.println(e.getErrorMsg());
        }
    }

    private void fromTable(Bitmap bitmap) {
        ProgressDialog dialog = new ProgressDialog(this);
        dialog.setMessage("解析中。。。");
        dialog.show();
        new Thread(new Runnable() {
            @Override
            public void run() {
                String base64 = bitmapToBase64(bitmap);
                RecognizeGeneralTableRequest request = new RecognizeGeneralTableRequest();
                GeneralTableRequestBody body = new GeneralTableRequestBody();
                body.withImage(base64);
                request.withBody(body);
                try {
                    RecognizeGeneralTableResponse response = base().recognizeGeneralTable(request);
                    GeneralTableResult result = response.getResult();

                    parseResult(result);
                    KLog.e("parse success ");

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            dialog.dismiss();
                            new AlertDialog.Builder(MainActivity.this)
                                    .setTitle("提示")
                                    .setMessage("解析完成")
                                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            Intent intent = new Intent(MainActivity.this, CarNumListActivity.class);
                                            intent.putExtra("data", parsedResultData);
                                            startActivity(intent);
                                        }
                                    })
                                    .show();
                        }
                    });
                } catch (ConnectionException | RequestTimeoutException e) {
                    e.printStackTrace();
                    KLog.e("run:" + e.getMessage());
                } catch (ServiceResponseException e) {
                    e.printStackTrace();
                    KLog.e("run: " + e.getHttpStatusCode() + " " + e.getErrorCode() + " " + e.getErrorMsg());
                }
            }
        }).start();
    }

    public void parseResult(GeneralTableResult result) {
        List<WordsRegionList> wordsRegionList = result.getWordsRegionList();

        //获取所有识别到的结果
        List<String> numList = new ArrayList<>();
        for (int i = 0; i < wordsRegionList.size(); i++) {
            WordsRegionList regionList = wordsRegionList.get(i);
            List<GeneralTableWordsBlockList> blockList = regionList.getWordsBlockList();
            for (int j = 0; j < blockList.size(); j++) {
                GeneralTableWordsBlockList list = blockList.get(j);
                String words = list.getWords();
                if (words.length() == 0) {
                    continue;
                }
                numList.add(words);
                KLog.e("raw word : " + words);
            }
        }

        List<String> ret = new ArrayList<>();
        for (int i = 0; i < numList.size(); i++) {
            String replace = numList.get(i).replace("\n", " ");
            if (replace.contains(" ")) {
                String[] s = replace.split(" ");
                for (String stt : s) {
                    if (stt.length() == 0) {
                        continue;
                    }
                    if (!isCarNum(stt)) {
                        continue;
                    }
                    ret.add(stt);
                    KLog.e("add multi car num:" + stt);
                }
            } else {
                if (!isCarNum(replace)) {
                    continue;
                }
                ret.add(replace);
                KLog.e("add multi car num:" + replace);
            }
        }

        //车牌排序
        parsedResultData = ret.toArray(new String[0]);
        Arrays.sort(parsedResultData);

        StringBuilder builder2 = new StringBuilder();
        for (int j = 0; j < parsedResultData.length; j++) {
            builder2.append(j < 9 ? "0" : "")
                    .append(j + 1)
                    .append("--->")
                    .append(parsedResultData[j])
                    .append("\n");
        }
        showResultOnView(builder2.toString());
    }

    public String bitmapToBase64(Bitmap bit) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bit.compress(Bitmap.CompressFormat.JPEG, 100, bos);
        byte[] bytes = bos.toByteArray();
        return Base64.encodeToString(bytes, Base64.DEFAULT);
    }

    public Bitmap base64ToBitmap(String base64) {
        byte[] bytes = Base64.decode(base64, Base64.DEFAULT);
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        return bitmap;
    }

    private void showResultOnView(String result) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
//                mResultTv.setText(result);
                mResultTv.append("\n");
                mResultTv.append(result);
            }
        });
    }

    private boolean isCarNum(String carNum) {
        //带"粤"
        if (carNum.length() < 6) {
            return false;
        }

        //不带"粤"
        boolean success = true;
        for (int i = 0; i < carNum.length(); i++) {
            char ch = carNum.charAt(i);
            if (i == 0) {
                if (ch >= 'A' && ch <= 'Z') {

                } else {
                    return false;
                }
            }

            if (ch >= '0' && ch <= '9') {

            } else if (ch >= 'A' && ch <= 'Z') {

            } else {
                success = false;
                break;
            }
        }
        return success;
    }
}