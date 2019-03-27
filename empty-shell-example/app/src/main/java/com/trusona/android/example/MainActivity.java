package com.trusona.android.example;

import android.os.Bundle;

import com.trusona.android.sdk.Trusona;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

  private Trusona trusona; // purposefully  not initialized here

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
  }
}