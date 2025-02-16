package com.example.mainactivity;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
public class SignLanguageAPI {
    @POST("http://127.0.0.1:8000/predict-gesture")
        // Replace with your API endpoint
    Call<ApiResponse> predictSignLanguage(@Body ImageRequest imageRequest) {
        return null;
    }
}
