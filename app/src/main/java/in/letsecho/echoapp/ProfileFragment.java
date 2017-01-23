package in.letsecho.echoapp;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import in.letsecho.echoapp.library.UserProfile;

public class ProfileFragment extends DialogFragment {

    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mUsersDbRef;
    private String mUserId;
    private View mView;
    private ImageView mPhotoImageView;
    private TextView mNameTextView;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = this.getActivity().getLayoutInflater();
        mView = inflater.inflate(R.layout.fragment_profile, null);
        builder.setView(mView);
        // Get mView items
        mPhotoImageView = (ImageView) mView.findViewById(R.id.displayImageView);
        mNameTextView = (TextView) mView.findViewById(R.id.nameTextView);
        return builder.create();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle bundle = getArguments();
        mUserId = bundle.getString("secondaryUserId");

        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mUsersDbRef = mFirebaseDatabase.getReference("users").child(mUserId);
        mUsersDbRef.addListenerForSingleValueEvent((new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot userObj) {
                UserProfile user = userObj.getValue(UserProfile.class);
                if (user.getPhotoUrl() != null) {
                    Glide.with(mPhotoImageView.getContext())
                            .load(user.getPhotoUrl())
                            .into(mPhotoImageView);
                }
                mNameTextView.setText(user.getName());
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {}
        }));

    }
}
