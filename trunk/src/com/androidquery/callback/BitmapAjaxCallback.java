/*
 * Copyright 2011 - AndroidQuery.com (tinyeeliu@gmail.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.androidquery.callback;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.View;
import android.widget.ImageView;

import com.androidquery.callback.AjaxCallback;
import com.androidquery.callback.AjaxStatus;
import com.androidquery.util.AQUtility;
import com.androidquery.util.Cache;

public class BitmapAjaxCallback extends AjaxCallback<Bitmap>{

	private static int SMALL_MAX = 20;
	private static int BIG_MAX = 20;
	
	private static Map<String, Bitmap> smallCache;
	private static Map<String, Bitmap> bigCache;
	
	private static HashMap<String, WeakHashMap<ImageView, Void>> ivsMap = new HashMap<String, WeakHashMap<ImageView, Void>>();	
	private WeakHashMap<ImageView, Void> ivs;
	
	
	public BitmapAjaxCallback(){
		ivs = new WeakHashMap<ImageView, Void>();			
	}
	
	public void setImageView(String url, ImageView view){
		
		presetBitmap(view, url);		
		ivs.put(view, null);
	}
	
	@Override
	public Bitmap fileGet(String url, File file, AjaxStatus status) {
		return BitmapFactory.decodeFile(file.getAbsolutePath());
	}
	
	@Override
	public Bitmap transform(String url, byte[] data, AjaxStatus status) {
		return BitmapFactory.decodeByteArray(data, 0, data.length);
	}
	
	@Override
	public final void callback(String url, Bitmap bm, AjaxStatus status) {
		
		ivsMap.remove(url);
		
		Set<ImageView> set = ivs.keySet();
		
		AQUtility.debug("concurrent", ivsMap.size());
		
		for(ImageView iv: set){
			if(iv != null && url.equals(iv.getTag())){
				callback(url, iv, bm, status);
			}
		}
		
		
	}
	
	
	protected void callback(String url, ImageView iv, Bitmap bm, AjaxStatus status){

		showBitmap(iv, bm);
	}

	public static void setIconCacheLimit(int limit){
		SMALL_MAX = limit;
		clearCache();
	}
	
	public static void setCacheLimit(int limit){
		BIG_MAX = limit;
		clearCache();
	}
	
	public static void clearCache(){
		bigCache = null;
		smallCache = null;
	}
	
	protected static void clearTasks(){
		ivsMap.clear();
	}
	
	private static Map<String, Bitmap> getBImgCache(){
		if(bigCache == null){
			bigCache = new Cache<String, Bitmap>(BIG_MAX);
		}
		return bigCache;
	}
	
	
	private static Map<String, Bitmap> getSImgCache(){
		if(smallCache == null){
			smallCache = new Cache<String, Bitmap>(SMALL_MAX);
		}
		return smallCache;
	}
	
	@Override
	public Bitmap memGet(String url){		
		return memGet2(url);
	}
	
	private static Bitmap memGet2(String url){
		
		Map<String, Bitmap> cache = getBImgCache();
		Bitmap result = cache.get(url);
		
		if(result == null){
			cache = getSImgCache();
			result = cache.get(url);
		}

		return result;
	}
	
	@Override
	public void memPut(String url, Bitmap bm){
		
		if(bm == null) return;
		
		int width = bm.getWidth();
				
		Map<String, Bitmap> cache = null;
		
		if(width > 50){
			cache = getBImgCache();
		}else{
			cache = getSImgCache();
		}
		
		cache.put(url, bm);
		
	}
	
	private static void showBitmap(ImageView iv, Bitmap bm){
		iv.setVisibility(View.VISIBLE);
		iv.setImageBitmap(bm);
	}
	
	private static void setBitmap(ImageView iv, String url, Bitmap bm){
		
		iv.setTag(url);
		
		if(bm != null){			
			showBitmap(iv, bm);
		}else{
			iv.setImageBitmap(null);	
		}
		
	}
	
	private static void presetBitmap(ImageView iw, String url){
		if(!url.equals(iw.getTag())){
			iw.setImageBitmap(null);
			iw.setTag(url);
		}
	}
	
	public static void async(Context context, ImageView iv, String url, boolean memCache, boolean fileCache){
		
		if(iv == null) return;
		
		//invalid url
		if(url == null || url.length() < 4){
			setBitmap(iv, null, null);
			return;
		}
		
		presetBitmap(iv, url);
		
		//check memory
		Bitmap bm = memGet2(url);
		if(bm != null){
			showBitmap(iv, bm);
			return;
		}
		
		WeakHashMap<ImageView, Void> ivs = ivsMap.get(url);
		
		if(ivs == null){		
			BitmapAjaxCallback cb = new BitmapAjaxCallback();
			cb.setImageView(url, iv);
			cb.start(context, url, memCache, fileCache);
		}else{
			ivs.put(iv, null);
		}
	}
	
	private void start(Context context, String url, boolean memCache, boolean fileCache){
		
		
		ivsMap.put(url, ivs);
		
		super.async(context, url, memCache, fileCache, false);
	}
	
	
}
