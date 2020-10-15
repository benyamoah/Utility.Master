package com.keren.utility;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class WorkerLoginRegisterActivity<FirebaseUser> extends AppCompatActivity {

    private TextView CreateWorkerAccount;
    private TextView TitleWorker;
    private Button LoginWorkerButton;
    private Button RegisterWorkerButton;
    private EditText WorkerEmail;
    private EditText WorkerPassword;

    private DatabaseReference WorkersDatabaseRef;
    private String onlineWorkerID;
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener firebaseAuthListener;

    private ProgressDialog loadingBar;

    private FirebaseUser currentUser;
    String currentUserID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_worker_login_register);

        mAuth = FirebaseAuth.getInstance();

        firebaseAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                currentUser = (FirebaseUser) FirebaseAuth.getInstance().getCurrentUser();

                if (currentUser != null) {
                    Intent intent = new Intent(WorkerLoginRegisterActivity.this, WorkersMapActivity.class);
                    startActivity(intent);
                }
            }
        };


        CreateWorkerAccount = (TextView) findViewById(R.id.create_worker_account);
        TitleWorker = (TextView) findViewById(R.id.titlr_worker);
        LoginWorkerButton = (Button) findViewById(R.id.login_worker_btn);
        RegisterWorkerButton = (Button) findViewById(R.id.register_worker_btn);
        WorkerEmail = (EditText) findViewById(R.id.worker_email);
        WorkerPassword = (EditText) findViewById(R.id.worker_password);
        loadingBar = new ProgressDialog(this);


        RegisterWorkerButton.setVisibility(View.INVISIBLE);
        RegisterWorkerButton.setEnabled(false);

        CreateWorkerAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CreateWorkerAccount.setVisibility(View.INVISIBLE);
                LoginWorkerButton.setVisibility(View.INVISIBLE);
                TitleWorker.setText("Worker Registration");

                RegisterWorkerButton.setVisibility(View.VISIBLE);
                RegisterWorkerButton.setEnabled(true);
            }
        });

        RegisterWorkerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String email = WorkerEmail.getText().toString();
                String password = WorkerPassword.getText().toString();

                if (TextUtils.isEmpty(email)) {
                    Toast.makeText(WorkerLoginRegisterActivity.this, "Please write your Email...", Toast.LENGTH_SHORT).show();
                }

                if (TextUtils.isEmpty(password)) {
                    Toast.makeText(WorkerLoginRegisterActivity.this, "Please write your Password...", Toast.LENGTH_SHORT).show();
                } else {
                    loadingBar.setTitle("Please wait :");
                    loadingBar.setMessage("While system is processing your data...");
                    loadingBar.show();

                    mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                onlineWorkerID = mAuth.getCurrentUser().getUid();
                                currentUserID = mAuth.getCurrentUser().getUid();
                                WorkersDatabaseRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Workers").child(currentUserID);
                                WorkersDatabaseRef.setValue(true);

                                Intent intent = new Intent(WorkerLoginRegisterActivity.this, WorkersMapActivity.class);
                                startActivity(intent);

                                loadingBar.dismiss();
                            } else {
                                Toast.makeText(WorkerLoginRegisterActivity.this, "Please Try Again. Error Occurred, while registering... ", Toast.LENGTH_SHORT).show();

                                loadingBar.dismiss();
                            }
                        }
                    });
                }
            }
        });


        LoginWorkerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String email = WorkerEmail.getText().toString();
                String password = WorkerPassword.getText().toString();

                if (TextUtils.isEmpty(email)) {
                    Toast.makeText(WorkerLoginRegisterActivity.this, "Please write your Email...", Toast.LENGTH_SHORT).show();
                }

                if (TextUtils.isEmpty(password)) {
                    Toast.makeText(WorkerLoginRegisterActivity.this, "Please write your Password...", Toast.LENGTH_SHORT).show();
                } else {
                    loadingBar.setTitle("Please wait :");
                    loadingBar.setMessage("While system is processing your data...");
                    loadingBar.show();

                    mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful())
                            {
                                Toast.makeText(WorkerLoginRegisterActivity.this, "Sign In , Successful...", Toast.LENGTH_SHORT).show();

                                Intent intent = new Intent(WorkerLoginRegisterActivity.this, WorkersMapActivity.class);
                                startActivity(intent);
                            }
                            else
                                {
                                Toast.makeText(WorkerLoginRegisterActivity.this, "Error Occurred, while Signing In... ", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }
            }
        });
    }
}
//    @Override
//  protected void onStart() {
//  super.onStart();
//
//    mAuth.addAuthStateListener(firebaseAuthListner);
//   }
//
//
//   @Override
//   protected void onStop()
//  {
//      super.onStop();
//
//       mAuth.removeAuthStateListener(firebaseAuthListner);
//    }
//}



