package com.vaccarostudio.bitcoinblockexplorer.activities;

import android.app.ProgressDialog;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.StoredBlock;

import java.util.LinkedHashMap;

import com.vaccarostudio.bitcoinblockexplorer.MySnackbar;
import com.vaccarostudio.bitcoinblockexplorer.R;
import com.vaccarostudio.bitcoinblockexplorer.insight.Block;
import com.vaccarostudio.bitcoinblockexplorer.insight.Insight;
import com.vaccarostudio.bitcoinblockexplorer.insight.Tx;
import com.vaccarostudio.bitcoinblockexplorer.insight.ValueIn;
import com.vaccarostudio.bitcoinblockexplorer.insight.ValueOut;
import com.vaccarostudio.bitcoinblockexplorer.adapters.ItemAdapter;

import static com.vaccarostudio.bitcoinblockexplorer.Bitcoin.blockStore;


public class TxActivity extends AppCompatActivity {

    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    private final LinkedHashMap<String, String> mDataset = new LinkedHashMap<>();

    String tx_address;
    Tx tx;
    Block block;
    StoredBlock storedBlock;
    String merkleRoot;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_items);

        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mRecyclerView.setHasFixedSize(true);

        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);

        DividerItemDecoration horizontalDecoration = new DividerItemDecoration(mRecyclerView.getContext(),
                DividerItemDecoration.VERTICAL);
        Drawable horizontalDivider = ContextCompat.getDrawable(this, R.drawable.divider_grey);
        horizontalDecoration.setDrawable(horizontalDivider);
        mRecyclerView.addItemDecoration(horizontalDecoration);

        // specify an adapter (see also next example)
        mAdapter = new ItemAdapter(mDataset);
        mRecyclerView.setAdapter(mAdapter);

        try {
            getTx();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void getTx() throws Exception {

        try {
            tx_address = getIntent().getStringExtra("tx");
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception();
        }


        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... voids) {
                try {
                    tx = null;
                    block=null;

                    // Get tx from insight
                    tx = Insight.getTx(tx_address);

                    // Get block from insight
                    block = Insight.getBlock(tx.blockhash);
                    merkleRoot = Block.merkle(block.transactions);

                    // Get block from blockstore
                    Sha256Hash hash = new Sha256Hash(block.hash);
                    storedBlock = blockStore.get(hash);

                } catch (Exception e) {
                    e.printStackTrace();
                }
                return true;
            }

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                setStatus("Verification Pending");
                progress(true);
            }
            @Override
            protected void onPostExecute(Boolean aBoolean) {
                super.onPostExecute(aBoolean);
                progress(false);
                if (aBoolean == false) {
                    MySnackbar.showNegative(TxActivity.this, "Sorry! Tx not found");
                    setStatus("Verification Failed");
                    return;
                }
                if (tx == null) {
                    MySnackbar.showNegative(TxActivity.this, "Sorry! Tx not found");
                    setStatus("Verification Failed");
                    return;
                }

                refresh();

                if (block == null) {
                    MySnackbar.showNegative(TxActivity.this, "Sorry! Block not found");
                    setStatus("Verification Failed");
                    return;
                }
                if (storedBlock == null){
                    MySnackbar.showWarning(TxActivity.this, "Blockchain not sync");
                    return;
                }

                if(merkleRoot.equals(block.merkleroot)){
                    MySnackbar.showPositive(TxActivity.this, "Merkle Root Verified");
                    setStatus("Verification Success");
                } else {
                    MySnackbar.showNegative(TxActivity.this, "Merkle Root Invalid");
                    setStatus("Verification Failed");
                }
            }
        }.execute();
    }



    private void refresh() {

        mDataset.clear();

        if (tx == null) {
            return;
        }

        if (tx.txid != null) {
            mDataset.put("txid", String.valueOf(tx.txid));
        }
        if (tx.blockhash != null) {
            mDataset.put("blockhash", String.valueOf(tx.blockhash));
        }
        if (tx.blockheight != null) {
            mDataset.put("blockheight", String.valueOf(tx.blockheight));
        }
        if (tx.blocktime != null) {
            mDataset.put("blocktime", String.valueOf(tx.blocktime));
        }
        if (tx.confirmations != null) {
            mDataset.put("confirmations", String.valueOf(tx.confirmations));
        }
        if (tx.fees != null) {
            mDataset.put("fees", String.valueOf(tx.fees));
        }
        if (tx.size != null) {
            mDataset.put("size", String.valueOf(tx.size));
        }
        if (tx.time != null) {
            mDataset.put("time", String.valueOf(tx.time));
        }
        if (tx.version != null) {
            mDataset.put("version", String.valueOf(tx.version));
        }

        if (tx.vin != null) {
            for (ValueIn vin : tx.vin) {
                mDataset.put("Vin", vin.toString());
            }
        }
        if (tx.vout != null) {
            for (ValueOut vout : tx.vout) {
                mDataset.put("Vout", vout.toString());
            }
        }
        mAdapter.notifyDataSetChanged();
    }


    public void setStatus(String message){
        ((TextView)findViewById(R.id.tvStatus)).setText(message);
    }



    private ProgressDialog mProgressDialog;
    public void progress(boolean status){
        if(mProgressDialog==null){
            mProgressDialog = new ProgressDialog(this);
            mProgressDialog.setMessage("Downloading........");
            mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            mProgressDialog.setCancelable(false);
        }
        if(status==true){
            mProgressDialog.show();
        }else{
            mProgressDialog.hide();
        }
    }
}
