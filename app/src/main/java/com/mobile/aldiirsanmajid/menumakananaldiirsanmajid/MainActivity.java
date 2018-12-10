package com.mobile.aldiirsanmajid.menumakananaldiirsanmajid;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.provider.DocumentsContract;
import android.support.annotation.NonNull;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public class MainActivity extends AppCompatActivity {

    private static final String urlListMenu = "https://www.dropbox.com/s/mekrh6h6b4i6u4j/menumakanan.xml?dl=1";
    private List<Item> listData;
    private GridView gridView;
    private TextView txtTotal;
    private SwipeRefreshLayout swipe;
    private int totalHarga = 0;
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private GoogleSignInClient mGoogleSignInClient;
    private GoogleApiClient mGoogleApiClient;

    final String TAG = this.getClass().getName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Untuk Log Out Dari Firebase dan Google
        mAuth = FirebaseAuth.getInstance();
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                if(firebaseAuth.getCurrentUser() == null){
                    startActivity(new Intent(MainActivity.this, Login_Activity.class));
                }
            }
        };
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        //tampilan
        txtTotal = (TextView)findViewById(R.id.total);
        gridView = (GridView)findViewById(R.id.gridViewMenu);
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                popUpItem(listData.get(position));
            }
        });

        if(listData == null){
            new MyTask().execute(urlListMenu);
        }

        //untuk refresh dengan swipe
        swipe = (SwipeRefreshLayout)findViewById(R.id.swipeMenu);
        swipe.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                txtTotal.setText("Total : Rp.0");
                new MyTask().execute(urlListMenu);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    //ketika icon di action bar di sentuh
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.checkout:
                AlertDialog.Builder Report = new AlertDialog.Builder(MainActivity.this);
                Report.setTitle("Total Pembelian");
                Report
                        .setMessage("Total Semua = "+ totalHarga)
                        .setPositiveButton("Pesan", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                totalHarga = 0;
                                setTotal(0);
                                Toast.makeText(MainActivity.this, "Makanan berhasil dipesan.", Toast.LENGTH_SHORT).show();

                            }
                        })
                        .setNegativeButton("Batal", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        });

                AlertDialog mAlertDialog = Report.create();
                mAlertDialog.show();
                break;

            case R.id.signOut:
                signOut();
                //mGoogleApiClient.clearDefaultAccountAndReconnect();

                break;

                default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    //log out akun
    private void signOut() {
        // Firebase sign out
        mAuth.signOut();

        // Google sign out
        mGoogleSignInClient.signOut().addOnCompleteListener(this,
                new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        Toast.makeText(MainActivity.this, "Berhasil Logout", Toast.LENGTH_SHORT).show();

                    }
                });
    }



    //untuk keluar tekan tombol kembali dua kali
    boolean twice;
    @Override
    public void onBackPressed() {
        Log.d(TAG, "click");

        if(twice == true){
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_HOME);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
            System.exit(0);
        }
        twice = true;
        Log.d(TAG,"twice : " + twice);
        Toast.makeText(MainActivity.this, "Silahkan Tekan Lagi Untuk Keluar", Toast.LENGTH_SHORT).show();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                twice = false;
                Log.d(TAG,"twice : " + twice);
            }
        }, 2000);
        twice = true;
    }


    @Override
    protected void onStart() {
        super.onStart();

        mAuth.addAuthStateListener(mAuthListener);
    }

    //progress
    class MyTask extends AsyncTask<String, Void, Void> {
        ProgressDialog pDialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            pDialog = new ProgressDialog(MainActivity.this);
            pDialog.setMessage("Loading Menu");
            pDialog.setCancelable(false);
            pDialog.show();
        }

        @Override
        protected Void doInBackground(String... params) {
            listData = getData(params[0]);
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            if(null != pDialog && pDialog.isShowing()){
                pDialog.dismiss();
            }

            swipe.setRefreshing(false);
            if(null == listData || listData.size() == 0){
                Toast.makeText(MainActivity.this, "Menu Tidak Ditemukan", Toast.LENGTH_LONG).show();
            } else {
                gridView.setAdapter(new CustomAdapterGridview(MainActivity.this, R.layout.gridview_layout, listData));
            }

            setTotal(0);
        }
    }

    //data dropbox ke List
    public List<Item> getData(String url){
        Item objItem;
        List<Item> listItem = null;

        try {
            listItem = new ArrayList<>();
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(new URL(url).openStream());
            doc.getDocumentElement().normalize();
            NodeList nList = doc.getElementsByTagName("item");

            int batas = nList.getLength();

            for (int temp = 0; temp < batas; temp++) {
                {
                Node nNode = nList.item(temp);
                if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element eElement = (Element) nNode;
                    objItem = new Item();
                    objItem.setNama(getTagValue("nama", eElement));
                    objItem.setHarga(getTagValue("harga", eElement));
                    objItem.setLink(getTagValue("link", eElement));
                    listItem.add(objItem);
                }
            }
        }
        } catch (Exception e){
            e.printStackTrace();
        }
        return listItem;
    }


    private static String getTagValue(String sTag, Element eElement){
        NodeList nlList = eElement.getElementsByTagName(sTag).item(0).getChildNodes();
        Node nValue = nlList.item(0);
        return nValue.getNodeValue();
    }

    //ketika item di sentuh
    private void popUpItem(final Item data){
        final AlertDialog.Builder alerDialog = new AlertDialog.Builder(MainActivity.this);

        LayoutInflater inflater = getLayoutInflater();
        View convertView = inflater.inflate(R.layout.item_layout, null);
        ImageView imgItem = (ImageView)convertView.findViewById(R.id.imgItem);
        final TextView txtDesc = (TextView)convertView.findViewById(R.id.txtDesc);
        final EditText txtJml = (EditText) convertView.findViewById(R.id.txtJml);

        Glide.with(this).load(data.getLink()).into(imgItem);
        txtDesc.setText(data.getNama()+" | Rp."+data.getHarga());

        alerDialog.setView(convertView).setTitle("");
        final AlertDialog mAlertDialog = alerDialog.setPositiveButton("Pesan", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                int pesanan = Integer.valueOf(data.getHarga());
                if(!txtJml.getText().toString().equals("") && !txtJml.getText().toString().equals("0")){
                    pesanan = Integer.valueOf(txtJml.getText().toString()) * Integer.valueOf(data.getHarga());
                }
                setTotal(pesanan);
            }
        }).create();

        mAlertDialog.show();
    }

    //menghitung total
    private void setTotal(int nilai){

        totalHarga = totalHarga + nilai;
        txtTotal.setText("Total : Rp. "+totalHarga);
    }


}
