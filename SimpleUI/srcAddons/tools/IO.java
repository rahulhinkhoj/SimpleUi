package tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OptionalDataException;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.io.StreamCorruptedException;
import java.net.HttpURLConnection;
import java.net.URL;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources.NotFoundException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup.LayoutParams;

import com.squareup.picasso.Picasso;

/**
 * Android specific extensions to default {@link util.IOHelper} class
 * 
 */
public class IO extends util.IOHelper {

	private static final String LOG_TAG = "IO";

	public static Bitmap loadBitmapFromId(Context context, int bitmapId) {
		if (context == null) {
			Log.e(LOG_TAG, "Context was null!");
			return null;
		}
		InputStream is = context.getResources().openRawResource(bitmapId);
		BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
		bitmapOptions.inPreferredConfig = Bitmap.Config.RGB_565;
		try {
			Bitmap b = BitmapFactory.decodeStream(is, null, bitmapOptions);
			Log.i(LOG_TAG, "image loaded: " + b);
			return b;
		} catch (Exception ex) {
			Log.e("bitmap loading exeption", "" + ex);
			return null;
		}
	}

	/**
	 * use {@link IO#convertStreamToString(InputStream)} instead
	 * 
	 * @param stream
	 * @return
	 */
	@Deprecated
	public static String convertInputStreamToString(InputStream stream) {
		if (stream == null) {
			return null;
		}

		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					stream));
			StringBuilder sb = new StringBuilder();

			String line = null;

			while ((line = reader.readLine()) != null) {
				sb.append(line + "\n");
			}
			stream.close();
			return sb.toString();

		} catch (Exception e) {
			Log.e(LOG_TAG, "Could not convert input stream to string");
		}
		return null;
	}

	/**
	 * any type of image can be imported this way
	 * 
	 * @param imagePath
	 *            for example "/sdcard/abc.PNG"
	 * @return
	 */
	public static Bitmap loadBitmapFromFile(String imagePath) {
		BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
		bitmapOptions.inPreferredConfig = Bitmap.Config.RGB_565;
		return BitmapFactory.decodeFile(imagePath, bitmapOptions);
	}

	/**
	 * The Eclipse UI editor cant preview ressources loaded from the assets
	 * folder so a dummy bitmap is used instead
	 * 
	 * @param context
	 * @param id
	 * @return
	 */
	public static Bitmap loadBitmapFromIdInCustomView(View v, int id) {
		if (v.isInEditMode() || id == 0) {
			return ImageTransform.createDummyBitmap();
		} else {
			return IO.loadBitmapFromId(v.getContext(), id);
		}
	}

	public static Bitmap loadBitmapFromJar(Context context, String path) {
		try {
			InputStream stream = Thread.currentThread().getContextClassLoader()
					.getResource(path).openStream();
			return BitmapFactory.decodeStream(stream);
		} catch (Exception e) {
			Log.e(LOG_TAG, "Can't load bitmap from jar with path=" + path);

			try {
				return loadBitmapFromAssetsFolder(context, path);
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		}
		return null;
	}

	public static Bitmap loadBitmapFromFile(File file) {
		if (file == null || !file.exists()) {
			return null;
		}
		return loadBitmapFromFile(file.toString());
	}

	/**
	 * @param filePath
	 *            e.g.
	 *            Environment.getExternalStorageDirectory().getAbsolutePath() +
	 *            File.separator +"test.jpg"
	 * @param b
	 * @param jpgQuality
	 *            e.g. 90
	 * @return
	 */
	public static boolean saveImageToFile(String filePath, Bitmap b,
			int jpgQuality) {
		if (b == null) {
			Log.e(LOG_TAG, "saveImageToFile: Passed bitmap was null!");
			return false;
		}
		try {
			File f = newFile(filePath);
			FileOutputStream out = new FileOutputStream(f);
			b.compress(Bitmap.CompressFormat.PNG, jpgQuality, out);
			out.close();
			return f.exists();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * 
	 * @param url
	 * @return
	 */
	public static Bitmap loadBitmapFromURL(String url) {
		return loadBitmapFromUrl(SimpleUiApplication.getContext(), url);
	}

	public static Bitmap loadBitmapFromUrl(Context c, String url) {
		try {
			if (c != null) {
				return Picasso.with(c).load(url).get();
			} else {
				Log.w(LOG_TAG, "Context in loadBitmapFromUrl() "
						+ "was null, can't use Picasso");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		try {
			HttpURLConnection connection = (HttpURLConnection) new URL(url)
					.openConnection();
			int length = connection.getContentLength();
			if (length > -1) {
				BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
				bitmapOptions.inPreferredConfig = Bitmap.Config.RGB_565;
				return BitmapFactory.decodeStream(connection.getInputStream(),
						null, bitmapOptions);
			}
		} catch (Exception e) {
			Log.e(LOG_TAG, "Error while loading an image from an URL: ", e);
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * turns any view (or view composition) in a bitmap
	 * 
	 * @param v
	 *            the view to transform into the bitmap
	 * @return the bitmap with the correct size of the view
	 */
	public static Bitmap loadBitmapFromView(View v) {
		v.clearFocus();
		v.setPressed(false);
		if (v.getMeasuredHeight() <= 0) {
			// first calc the size the view will need:
			v.measure(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);

			// then create a bitmap to store the views drawings:
			Bitmap b = Bitmap.createBitmap(v.getMeasuredWidth(),
					v.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
			// wrap the bitmap:
			Canvas c = new Canvas(b);
			// set the view size to the mesured values:
			v.layout(0, 0, v.getWidth(), v.getHeight());
			// and draw the view onto the bitmap contained in the canvas:
			v.draw(c);
			return b;
		} else {
			Bitmap b = Bitmap.createBitmap(v.getWidth(), v.getHeight(),
					Bitmap.Config.ARGB_8888);
			Canvas c = new Canvas(b);
			v.draw(c);
			return b;
		}

	}

	/**
	 * @param context
	 * @param id
	 *            something like "R.drawable.icon"
	 * @return
	 */
	public static Drawable loadDrawableFromId(Context context, int id)
			throws NotFoundException {
		return context.getResources().getDrawable(id);
	}

	public static void saveStringToExternalStorage(String filename,
			String textToSave) throws IOException {

		File file = newFile(filename);

		FileOutputStream foStream = new FileOutputStream(file);
		OutputStreamWriter stringOut = new OutputStreamWriter(foStream);
		stringOut.write(textToSave);
		stringOut.close();
		foStream.close();
	}

	public static void saveSerializableToPrivateStorage(Context context,
			String filename, Serializable objectToSave) throws IOException {
		FileOutputStream fileOut = context.openFileOutput(filename,
				Context.MODE_PRIVATE);
		saveSerializableToStream(objectToSave, fileOut);
	}

	public static Object loadSerializableFromPrivateStorage(Context context,
			String filename) throws StreamCorruptedException,
			OptionalDataException, IOException, ClassNotFoundException {
		FileInputStream fiStream = context.openFileInput(filename);
		return loadSerializableFromStream(fiStream);
	}

	public static class Settings {

		Context context;
		private final String mySettingsName;
		/**
		 * The editor is stored as a field because every
		 * {@link SharedPreferences}.edit() call will create a new
		 * {@link Editor} object and this way resources are saved
		 */
		private Editor e;
		private int mode = Context.MODE_PRIVATE;

		public Settings(Context target, String settingsFileName) {
			context = target;
			mySettingsName = settingsFileName;
		}

		/**
		 * @param mode
		 *            default value is {@link Context}.MODE_PRIVATE
		 */
		public void setMode(int mode) {
			this.mode = mode;
		}

		public String loadString(String key) {
			return context.getSharedPreferences(mySettingsName, mode)
					.getString(key, null);
		}

		/**
		 * @param key
		 * @param defaultValue
		 *            the value which will be returned if there was no value
		 *            found for the given key
		 * @return
		 */
		public boolean loadBool(String key, boolean defaultValue) {
			return context.getSharedPreferences(mySettingsName, mode)
					.getBoolean(key, defaultValue);
		}

		/**
		 * @param key
		 * @param defaultValue
		 *            the value which will be returned if there was no value
		 *            found for the given key
		 * @return
		 */
		public int loadInt(String key, int defaultValue) {
			return context.getSharedPreferences(mySettingsName, mode).getInt(
					key, defaultValue);
		}

		public void storeString(String key, String value) {
			if (e == null) {
				e = context.getSharedPreferences(mySettingsName, mode).edit();
			}
			e.putString(key, value);
			e.commit();
		}

		public void storeBool(String key, boolean value) {
			if (e == null) {
				e = context.getSharedPreferences(mySettingsName, mode).edit();
			}
			e.putBoolean(key, value);
			e.commit();
		}

		public void storeInt(String key, int value) {
			if (e == null) {
				e = context.getSharedPreferences(mySettingsName, mode).edit();
			}
			e.putInt(key, value);
			e.commit();
		}
	}

	public static String getSDCardDirectory() {
		return Environment.getExternalStorageDirectory().toString();
	}

	/**
	 * @param relativePathInAssetsFolder
	 *            something like "folderX/fileY.txt" if you have a folder in
	 *            your assets folder folderX which contains a fileY.txt
	 * @return a file object which does not behave normally, e.g. file.exists()
	 *         will always return null! But {@link Picasso} e.g. still can
	 *         handle the file correctly
	 */
	public static Uri loadFileFromAssets(String relativePathInAssetsFolder) {
		if (!relativePathInAssetsFolder.startsWith("/")) {
			relativePathInAssetsFolder = "/" + relativePathInAssetsFolder;
		}
		return Uri.parse("file:///android_asset" + relativePathInAssetsFolder);
	}

	/**
	 * @Deprecated till android dependency fixed
	 * 
	 * @param oldPath
	 *            e.g. /myFolder/test.txt
	 * @param newName
	 *            e.g. oldTest.txt
	 * @return
	 */
	@Deprecated
	public static boolean renameFile(String oldPath, String newName) {
		File source = new File(Environment.getExternalStorageDirectory(),
				oldPath);
		File dest = new File(source.getParent(), newName);
		return source.renameTo(dest);
	}

	public static FileReader loadFileReaderFromAssets(Context c,
			String relativePathInAssetsFolder) {
		try {
			return new FileReader(c.getAssets()
					.openFd(relativePathInAssetsFolder).getFileDescriptor());
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static Bitmap loadBitmapFromAssetsFolder(Context context,
			String fileName) {
		try {
			Log.e(LOG_TAG, "Trying to load " + fileName
					+ " from assets folder!");
			BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
			bitmapOptions.inPreferredConfig = Bitmap.Config.RGB_565;
			return BitmapFactory.decodeStream(context.getAssets()
					.open(fileName), null, bitmapOptions);
		} catch (Exception e) {
			Log.e(LOG_TAG, "Could not load " + fileName
					+ " from assets folder!");
		}
		return null;
	}

	public static Uri toUri(File file) {
		return Uri.fromFile(file);
	}

	public static File toFile(Uri uri) {
		if (uri == null) {
			return null;
		}
		return new File(uri.getPath());
	}

	public static Bitmap loadBitmapFromUri(Uri uri) {
		if (uri == null) {
			return null;
		}
		return loadBitmapFromFile(toFile(uri));
	}

}
