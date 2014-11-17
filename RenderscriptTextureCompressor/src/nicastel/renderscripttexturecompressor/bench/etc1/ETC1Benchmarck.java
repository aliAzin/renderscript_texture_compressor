package nicastel.renderscripttexturecompressor.bench.etc1;

import gov.nasa.worldwind.util.dds.DDSCompressor;
import gov.nasa.worldwind.util.dds.DXTCompressionAttributes;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import nicastel.renderscripttexturecompressor.dds.ETC1Compressor;
import nicastel.renderscripttexturecompressor.dds.ETC1DDSCompressor;
import nicastel.renderscripttexturecompressor.dds.ETCConstants;
import nicastel.renderscripttexturecompressor.etc1.java.JavaETC1;
import nicastel.renderscripttexturecompressor.etc1.rs.RsETC1;
import nicastel.renderscripttexturecompressor.etc1.rs.ScriptC_etc1compressor;
import nicastel.renderscripttexturecompressor.pkm.PKMEncoder;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.opengl.ETC1;
import android.opengl.ETC1Util.ETC1Texture;
import android.support.v8.renderscript.Allocation;
import android.support.v8.renderscript.Allocation.MipmapControl;
import android.support.v8.renderscript.RenderScript;

public class ETC1Benchmarck {
	private final static int mask = 8;
	private static ByteBuffer compressedImage;
	private static ByteBuffer compressedImageAlpha;
	private static Bitmap bitmapARGB;
	private static Bitmap bitmap;
	private static ByteBuffer buffer;

	public static byte[] testSDKETC1BlockCompressor() {
		// Test android class block (reference)
		byte[] in1 = { 6, 5, 7, 7, 6, 5, 9, 2, 1, 20, 5, 80, 75, 24, 96, 64,
				27, 43, 45, 78, 21, 2, 85, 32, 9, 5, 7, 7, 6, 5, 9, 2, 1, 85,
				5, 80, 75, 3, 96, 64, 4, 43, 45, 78, 21, 2, 7, 32 };
		ByteBuffer inb = ByteBuffer.allocateDirect(48).order(
				ByteOrder.nativeOrder());
		inb.put(in1);
		ByteBuffer out = ByteBuffer.allocateDirect(8).order(
				ByteOrder.nativeOrder());

		inb.rewind();
		ETC1.encodeBlock(inb, mask, out);
		inb.rewind();

		byte[] arrayOut1 = new byte[8];
		out.get(arrayOut1);
		
		return arrayOut1;
	}

	public static byte[] testJavaETC1BlockCompressor() {
		// Test pure java block compressor
		byte[] in2 = { 6, 5, 7, 7, 6, 5, 9, 2, 1, 20, 5, 80, 75, 24, 96, 64,
				27, 43, 45, 78, 21, 2, 85, 32, 9, 5, 7, 7, 6, 5, 9, 2, 1, 85,
				5, 80, 75, 3, 96, 64, 4, 43, 45, 78, 21, 2, 7, 32 };

		byte[] arrayOut2 = new byte[8];

		JavaETC1.encodeBlock(in2, mask, arrayOut2);
		
		return arrayOut2;
	}

	public static void initBuffer() {
		InputStream input2 = PKMEncoder.class
				//.getResourceAsStream("/testdata/block.jpg");
				.getResourceAsStream("/testdata/world.topo.bathy.200405.3x256x128.jpg");
		// Wrap the stream in a BufferedInputStream to provide the
		// mark/reset capability required to
		// avoid destroying the stream when it is read more than once.
		// BufferedInputStream also improves
		// file read performance.
		if (!(input2 instanceof BufferedInputStream))
			input2 = new BufferedInputStream(input2);
		Options opts = new BitmapFactory.Options();
		opts.inPreferredConfig = Config.RGB_565;
		bitmap = BitmapFactory.decodeStream(input2, null, opts);
		if (bitmap != null) {
			buffer = ByteBuffer.allocateDirect(
					bitmap.getRowBytes() * bitmap.getHeight()).order(
					ByteOrder.nativeOrder());
			bitmap.copyPixelsToBuffer(buffer);
			buffer.position(0);

			System.out.println("Width : " + bitmap.getWidth());
			System.out.println("Height : " + bitmap.getHeight());
			System.out.println("Config : " + bitmap.getConfig());

//			if (bitmap.getConfig() == Bitmap.Config.ARGB_4444
//					|| bitmap.getConfig() == Bitmap.Config.ARGB_8888) {
//				System.out.println("Texture need aplha channel");
//				return;
//			}

			final int encodedImageSize = ETC1.getEncodedDataSize(
					bitmap.getWidth(), bitmap.getHeight());
			compressedImage = ByteBuffer.allocateDirect(encodedImageSize)
					.order(ByteOrder.nativeOrder());
			compressedImageAlpha = ByteBuffer.allocateDirect(encodedImageSize)
					.order(ByteOrder.nativeOrder());
		}
		try {
			input2.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		
		InputStream input = PKMEncoder.class
				//.getResourceAsStream("/testdata/block.jpg");
				.getResourceAsStream("/testdata/world.topo.bathy.200405.3x256x128.jpg");
		// Wrap the stream in a BufferedInputStream to provide the
		// mark/reset capability required to
		// avoid destroying the stream when it is read more than once.
		// BufferedInputStream also improves
		// file read performance.
		if (!(input instanceof BufferedInputStream))
			input = new BufferedInputStream(input);
		// stream.reset();
		// stream.mark(1024);
		Options opts2 = new BitmapFactory.Options();
		// opts.inPremultiplied = false;
		opts.inPreferredConfig = Config.ARGB_8888;
		bitmapARGB = BitmapFactory.decodeStream(input, null, opts);
	}

	public static ETC1Texture testSDKETC1ImageCompressor() {

		// RGB_565 is 2 bytes per pixel
		ETC1.encodeImage(buffer, bitmap.getWidth(), bitmap.getHeight(), 2,
				2 * bitmap.getWidth(), compressedImage);

		ETC1Texture texture = new ETC1Texture(bitmap.getWidth(),
				bitmap.getHeight(), compressedImage);

		buffer.rewind();		
		
		// if (texture != null) {
		// int estimatedMemorySize = ETC1.ETC_PKM_HEADER_SIZE
		// + texture.getHeight() * texture.getWidth() / 2;
		// File f = new
		// File(Environment.getExternalStorageDirectory(),"bmngpkm.pkm");
		// f.delete();
		// f.createNewFile();
		// ETC1Util.writeTexture(texture, new FileOutputStream(f));
		// System.out.println("Texture PKM created ");
		// }
		// System.out.println("Texture PKM creation failed ");
		
		return texture;
	}

	public static ETC1Texture testJavaETC1ImageCompressor() {
		// RGB_565 is 2 bytes per pixel
		JavaETC1.encodeImage(buffer, bitmap.getWidth(), bitmap.getHeight(), 2,
				2 * bitmap.getWidth(), compressedImage);

		ETC1Texture texture = new ETC1Texture(bitmap.getWidth(),
				bitmap.getHeight(), compressedImage);
		
		buffer.rewind();
		// if (texture != null) {
		// int estimatedMemorySize = ETC1.ETC_PKM_HEADER_SIZE
		// + texture.getHeight() * texture.getWidth() / 2;
		// File f = new
		// File(Environment.getExternalStorageDirectory(),"bmngpkm.pkm");
		// f.delete();
		// f.createNewFile();
		// ETC1Util.writeTexture(texture, new FileOutputStream(f));
		// System.out.println("Texture PKM created ");
		// }
		// System.out.println("Texture PKM creation failed ");
		
		return texture;
	}

	public static ETC1Texture testRsETC1ImageCompressor(RenderScript rs,
			ScriptC_etc1compressor script) {
		
		Allocation alloc = Allocation.createFromBitmap(rs, bitmapARGB, MipmapControl.MIPMAP_NONE, Allocation.USAGE_SHARED);

		// RGB_565 is 2 bytes per pixel
		RsETC1.encodeImage(rs, script, alloc, bitmapARGB.getWidth(), bitmapARGB.getHeight(), 4,
				4 * bitmapARGB.getWidth(), compressedImage, null, false, false);

		ETC1Texture texture = new ETC1Texture(bitmapARGB.getWidth(),
				bitmapARGB.getHeight(), compressedImage);
		
		alloc.destroy();
		
		// if (texture != null) {
		// int estimatedMemorySize = ETC1.ETC_PKM_HEADER_SIZE
		// + texture.getHeight() * texture.getWidth() / 2;
		// File f = new
		// File(Environment.getExternalStorageDirectory(),"bmngpkm.pkm");
		// f.delete();
		// f.createNewFile();
		// ETC1Util.writeTexture(texture, new FileOutputStream(f));
		// System.out.println("Texture PKM created ");
		// }
		// System.out.println("Texture PKM creation failed ");
		
		return texture;
	}
	
	public static ETC1Texture testRsETC1ImageCompressorWithAlpha(RenderScript rs,
			ScriptC_etc1compressor script) {
		
		Allocation alloc = Allocation.createFromBitmap(rs, bitmapARGB, MipmapControl.MIPMAP_NONE, Allocation.USAGE_SHARED);

		// RGB_565 is 2 bytes per pixel
		RsETC1.encodeImage(rs, script, alloc, bitmapARGB.getWidth(), bitmapARGB.getHeight(), 4,
				4 * bitmapARGB.getWidth(), compressedImage, compressedImageAlpha, false, true);

		ETC1Texture texture = new ETC1Texture(bitmapARGB.getWidth(),
				bitmapARGB.getHeight(), compressedImage);
		
		alloc.destroy();
		
		// if (texture != null) {
		// int estimatedMemorySize = ETC1.ETC_PKM_HEADER_SIZE
		// + texture.getHeight() * texture.getWidth() / 2;
		// File f = new
		// File(Environment.getExternalStorageDirectory(),"bmngpkm.pkm");
		// f.delete();
		// f.createNewFile();
		// ETC1Util.writeTexture(texture, new FileOutputStream(f));
		// System.out.println("Texture PKM created ");
		// }
		// System.out.println("Texture PKM creation failed ");
		
		return texture;
	}
	
	public static ByteBuffer testRsDDSETC1ImageCompressor(RenderScript rs,
			ScriptC_etc1compressor script) {
		// RGB_565 is 2 bytes per pixel
		ETC1DDSCompressor compressor = new ETC1DDSCompressor();
		
		DXTCompressionAttributes attributes = DDSCompressor.getDefaultCompressionAttributes();
		
		ETC1Compressor.rs = rs;
		ETC1Compressor.script = script;
		
		attributes.setDXTFormat(ETCConstants.D3DFMT_ETC1);
		ByteBuffer ddsBuffer = compressor.compressImage(bitmapARGB, attributes);
		
		// if (texture != null) {
		// int estimatedMemorySize = ETC1.ETC_PKM_HEADER_SIZE
		// + texture.getHeight() * texture.getWidth() / 2;
		// File f = new
		// File(Environment.getExternalStorageDirectory(),"bmngpkm.pkm");
		// f.delete();
		// f.createNewFile();
		// ETC1Util.writeTexture(texture, new FileOutputStream(f));
		// System.out.println("Texture PKM created ");
		// }
		// System.out.println("Texture PKM creation failed ");
		
		return ddsBuffer;
	}
}
