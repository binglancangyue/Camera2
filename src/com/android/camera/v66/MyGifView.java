package com.android.camera.v66;

import java.io.IOException;
import java.io.OutputStream;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Movie;
import android.util.AttributeSet;
import android.view.View;
import com.android.camera2.R;

public class MyGifView extends View{

	private long movieStart;

	private Movie movie;

	// 姝ゅ蹇呴』閲嶅啓璇ユ瀯閫犳柟娉�

	public MyGifView(Context context, AttributeSet attributeSet) {

		super(context, attributeSet);

		// 浠ユ枃浠舵祦锛圛nputStream锛夎鍙栬繘gif鍥剧墖璧勬簮

		movie = Movie.decodeStream(getResources().openRawResource(
				R.drawable.keyboard));

	}

	@Override
	protected void onDraw(Canvas canvas) {

		long curTime = android.os.SystemClock.uptimeMillis();

		// 绗竴娆℃挱鏀�

		if (movieStart == 0) {

			movieStart = curTime;

		}

		if (movie != null) {

			int duraction = movie.duration();

			int relTime = (int) ((curTime - movieStart) % duraction);

			movie.setTime(relTime);

			movie.draw(canvas, 0, 0);

			// 寮哄埗閲嶇粯

			invalidate();

		}

		super.onDraw(canvas);

	}
}
