package com.coolweather.app.activity;

import java.util.ArrayList;
import java.util.List;

import com.coolweather.app.R;
import com.coolweather.app.model.City;
import com.coolweather.app.model.CoolWeatherDB;
import com.coolweather.app.model.County;
import com.coolweather.app.model.Province;
import com.coolweather.app.util.HttpCallBackListener;
import com.coolweather.app.util.HttpUtil;
import com.coolweather.app.util.Utilty;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;


public class ChooseAreaActivity extends Activity {
	
	public static final int LEVEL_PROVINCE = 0;
	public static final int LEVEL_CITY = 1;
	public static final int LEVEL_COUNTY = 2;
	
	private ProgressDialog progressDialog;
	private TextView titleText;
	private ListView listView;
	private ArrayAdapter<String> adapter;
	private CoolWeatherDB coolWeatherDB;
	private List<String> dataList = new ArrayList<String>();
	
	private List<Province> provinceList;
	private Province selectedProvince;
	
	private List<City> cityList;
	private City selectedCity;
	
	private List<County> countyList;
	private County selectedCounty;
	
	private int crrentLevel;
	
	private boolean isWeather;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        isWeather = getIntent().getBooleanExtra("form_weather_activity", false);
        
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if(prefs.getBoolean("city_selected", false) && !isWeather){
        	Intent intent = new Intent(this,WeatherActivity.class);
        	startActivity(intent);
        	finish();
        	return;
        }
        
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.choose_area);
        listView = (ListView)findViewById(R.id.list_view);
        titleText = (TextView)findViewById(R.id.title_text);
        adapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,dataList);
        listView.setAdapter(adapter);
        coolWeatherDB = CoolWeatherDB.getInstance(this);
        listView.setOnItemClickListener(new OnItemClickListener(){

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				if(crrentLevel == LEVEL_PROVINCE){
					selectedProvince = provinceList.get(position);
					querCities();
				}else if(crrentLevel == LEVEL_CITY){
					selectedCity = cityList.get(position);
					querCounties();
				}else if(crrentLevel == LEVEL_COUNTY){
					String countyCode = countyList.get(position).getCountyCode();
					Intent intent = new Intent(ChooseAreaActivity.this,WeatherActivity.class);
					intent.putExtra("county_code", countyCode);
					startActivity(intent);
		        	finish();
				}
				
			}
        	
        });
        querProvinces();
    }

    private void querProvinces(){
    	provinceList = coolWeatherDB.loadProvinces();
    	if(provinceList.size()>0){
    		dataList.clear();
    		for(Province p : provinceList){
    			dataList.add(p.getProvinceName());
    		}
    		adapter.notifyDataSetChanged();
    		listView.setSelection(0);
    		titleText.setText("中国");
    		crrentLevel = LEVEL_PROVINCE;
    	}else{
    		queryFormServer(null,"province");
    	}
    }
    
    private void querCities(){
    	cityList = coolWeatherDB.loadCities(selectedProvince.getId());
    	if(cityList.size()>0){
    		dataList.clear();
    		for(City c : cityList){
    			dataList.add(c.getCityName());
    		}
    		adapter.notifyDataSetChanged();
    		listView.setSelection(0);
    		titleText.setText(selectedProvince.getProvinceName());
    		crrentLevel = LEVEL_CITY;
    	}else{
    		queryFormServer(selectedProvince.getProvinceCode(),"city");
    	}
    
    }

    private void querCounties(){
    	countyList = coolWeatherDB.loadCounties(selectedCity.getId());
    	if(countyList.size()>0){
    		dataList.clear();
    		for(County C : countyList){
    			dataList.add(C.getCountyName());
    		}
    		adapter.notifyDataSetChanged();
    		listView.setSelection(0);
    		titleText.setText(selectedCity.getCityName());
    		crrentLevel = LEVEL_COUNTY;
    	}else{
    		queryFormServer(selectedCity.getCityCode(),"county");
    	}

    }
    
    private void queryFormServer (final String code,final String type){
    	String address;
    	if(!TextUtils.isEmpty(code)){
    		address = "http://www.weather.com,cn/data/list3/city"+code+".xml";
    	}else{
    		address = "http://www.weather.com,cn/data/list3/city.xml";
    	}
    	showProgressDialog();
    	HttpUtil.sendHttpRequest(address, new HttpCallBackListener(){

			@Override
			public void onFinish(String response) {
				boolean result = false;
				if("province".equals(type)){
					result = Utilty.handleProvinceResponse(coolWeatherDB, response);
				}else if("city".equals(type)){
					result = Utilty.handleCitiesResponse(coolWeatherDB, response, selectedProvince.getId());
				}else{
					result = Utilty.handleCountiesResponse(coolWeatherDB, response, selectedCity.getId());
				}
				
				if(result){
					runOnUiThread(new Runnable(){

						@Override
						public void run() {
							closeProgressDialog();
							if("province".equals(type)){
								querProvinces();
							}else if("city".equals(type)){
								querCities();
							}else{
								querCounties();
							}
						}
						
					});
				}
				
			}

			@Override
			public void onError(Exception e) {
				runOnUiThread(new Runnable(){

					@Override
					public void run() {
						closeProgressDialog();
						Toast.makeText(ChooseAreaActivity.this, "加载失败",Toast.LENGTH_SHORT).show();
					}
					
				});
				
			}
    		
    	});
    }

	private void showProgressDialog() {
		if(progressDialog == null){
			progressDialog = new ProgressDialog(this);
			progressDialog.setMessage("正在加载...");
			progressDialog.setCanceledOnTouchOutside(false);
		}
		progressDialog.show();
	}
	
	private void closeProgressDialog() {
		if(progressDialog != null){
			progressDialog.dismiss();
		}
	}
	
	@Override
	public void onBackPressed() {
		if(crrentLevel == LEVEL_COUNTY){
			querCities();
		}else if(crrentLevel == LEVEL_CITY){
			querProvinces();
		}else{
			if(isWeather){
				Intent intent = new Intent(this,WeatherActivity.class);
				startActivity(intent);
			}
			querCounties();
		}
	}
   
}
