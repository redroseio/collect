package com.redrosecps.collect.android.activities;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import com.redrosecps.collect.android.R;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application.ActivityLifecycleCallbacks;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.TagLostException;
import android.nfc.tech.MifareClassic;
import android.nfc.tech.NfcA;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Parcel;
import android.provider.Settings;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Base64;
import android.util.Log;
import android.util.Pair;
import android.util.SparseArray;
import android.widget.TextView;
import android.widget.Toast;

public class ActivateBeneficiaryCardActivity extends Activity
{

	public static final String INTENT_DATA_BENEFICIARY_FULL_NAME = "beneficiary_full_name";
	public static final String INTENT_DATA_SLIP_COMPANY_NAME = "slip_company_name";
	public static final String INTENT_DATA_SLIP_HEADER_VOUCHER = "slip_header_voucher";
	public static final String INTENT_DATA_SLIP_HEADER_LOGO = "slip_header_logo";
	public static final String INTENT_DATA_SLIP_PRINT_FONT = "slip_print_font";
	public static final String INTENT_DATA_SLIP_PRINT_FONT_SIZE = "slip_print_font_size";
	public static final String INTENT_DATA_HOTLINE_NUMBER = "hotline_number";
	public static final String INTENT_DATA_READ_CARD = "read_card";

	public static final String INTENT_DATA_VALUE = "value";

	private class PrintParams
	{
		String slipCompanyName;
		String slipHeaderVoucher;
		String logo;
		String printFont;
		String printFontSize;
		String hotlineNumber;
	}

	private Random rng;
	protected NFCReceiver nfcReceiver;
	private TextView beneficiaryFullName;
	private boolean readCard;
	private PrintParams printParams = new PrintParams();

	/**
	 * Provides functions to read/write/analyze a Mifare Classic tag.
	 * @author Gerhard Klostermeier
	 */
	public static class MCReader
	{

		private static final String LOG_TAG = MCReader.class.getSimpleName();
		/**
		 * Placeholder for not found keys.
		 */
		public static final String NO_KEY = "------------";
		/**
		 * Placeholder for unreadable blocks.
		 */
		public static final String NO_DATA = "--------------------------------";

		private final MifareClassic mMFC;
		private SparseArray<byte[][]> mKeyMap = new SparseArray<byte[][]>();
		private int mKeyMapStatus = 0;
		private int mLastSector = -1;
		private int mFirstSector = 0;
		private ArrayList<byte[]> mKeysWithOrder;

		/**
		 * Initialize a Mifare Classic reader for the given tag.
		 * @param tag The tag to operate on.
		 */
		private MCReader(Tag tag)
		{
			MifareClassic tmpMFC = null;
			try
			{
				tmpMFC = MifareClassic.get(tag);
			}
			catch (Exception e)
			{
				Log.e(LOG_TAG, "Could not create Mifare Classic reader for the"
						+ "provided tag (even after patching it).");
				throw new RuntimeException(e);
			}
			mMFC = tmpMFC;
		}

		/**
		 * Patch a possibly broken Tag object of HTC One (m7/m8) or Sony
		 * Xperia Z3 devices (with Android 5.x.)
		 *
		 * HTC One: "It seems, the reason of this bug is TechExtras of NfcA is null.
		 * However, TechList contains MifareClassic." -- bildin.
		 * This method will fix this. For more information please refer to
		 * https://github.com/ikarus23/MifareClassicTool/issues/52
		 * This patch was provided by bildin (https://github.com/bildin).
		 *
		 * Sony Xperia Z3 (+ emmulated Mifare Classic tag): The buggy tag has
		 * two NfcA in the TechList with different SAK values and a MifareClassic
		 * (with the Extra of the second NfcA). Both, the second NfcA and the
		 * MifareClassic technique, have a SAK of 0x20. According to NXP's
		 * guidelines on identifying Mifare tags (Page 11), this a Mifare Plus or
		 * Mifare DESFire tag. This method creates a new Extra with the SAK
		 * values of both NfcA occurrences ORed (as mentioned in NXP's
		 * Mifare type identification procedure guide) and replace the Extra of
		 * the first NfcA with the new one. For more information please refer to
		 * https://github.com/ikarus23/MifareClassicTool/issues/64
		 * This patch was provided by bildin (https://github.com/bildin).
		 *
		 * @param tag The possibly broken tag.
		 * @return The fixed tag.
		 */
		public static Tag patchTag(Tag tag)
		{
			if (tag == null)
			{
				return null;
			}

			String[] techList = tag.getTechList();

			Parcel oldParcel = Parcel.obtain();
			tag.writeToParcel(oldParcel, 0);
			oldParcel.setDataPosition(0);

			int len = oldParcel.readInt();
			byte[] id = new byte[0];
			if (len >= 0)
			{
				id = new byte[len];
				oldParcel.readByteArray(id);
			}
			int[] oldTechList = new int[oldParcel.readInt()];
			oldParcel.readIntArray(oldTechList);
			Bundle[] oldTechExtras = oldParcel.createTypedArray(Bundle.CREATOR);
			int serviceHandle = oldParcel.readInt();
			int isMock = oldParcel.readInt();
			IBinder tagService;
			if (isMock == 0)
			{
				tagService = oldParcel.readStrongBinder();
			}
			else
			{
				tagService = null;
			}
			oldParcel.recycle();

			int nfcaIdx = -1;
			int mcIdx = -1;
			short sak = 0;
			boolean isFirstSak = true;

			for (int i = 0; i < techList.length; i++)
			{
				if (techList[i].equals(NfcA.class.getName()))
				{
					if (nfcaIdx == -1)
					{
						nfcaIdx = i;
					}
					if (oldTechExtras[i] != null && oldTechExtras[i].containsKey("sak"))
					{
						sak = (short)(sak | oldTechExtras[i].getShort("sak"));
						isFirstSak = (nfcaIdx == i) ? true : false;
					}
				}
				else if (techList[i].equals(MifareClassic.class.getName()))
				{
					mcIdx = i;
				}
			}

			boolean modified = false;

			// Patch the double NfcA issue (with different SAK) for
			// Sony Z3 devices.
			if (!isFirstSak)
			{
				oldTechExtras[nfcaIdx].putShort("sak", sak);
				modified = true;
			}

			// Patch the wrong index issue for HTC One devices.
			if (nfcaIdx != -1 && mcIdx != -1 && oldTechExtras[mcIdx] == null)
			{
				oldTechExtras[mcIdx] = oldTechExtras[nfcaIdx];
				modified = true;
			}

			if (!modified)
			{
				// Old tag was not modivied. Return the old one.
				return tag;
			}

			// Old tag was modified. Create a new tag with the new data.
			Parcel newParcel = Parcel.obtain();
			newParcel.writeInt(id.length);
			newParcel.writeByteArray(id);
			newParcel.writeInt(oldTechList.length);
			newParcel.writeIntArray(oldTechList);
			newParcel.writeTypedArray(oldTechExtras, 0);
			newParcel.writeInt(serviceHandle);
			newParcel.writeInt(isMock);
			if (isMock == 0)
			{
				newParcel.writeStrongBinder(tagService);
			}
			newParcel.setDataPosition(0);
			Tag newTag = Tag.CREATOR.createFromParcel(newParcel);
			newParcel.recycle();

			return newTag;
		}

		/**
		 * Get new instance of {@link MCReader}.
		 * If the tag is "null" or if it is not a Mifare Classic tag, "null"
		 * will be returned.
		 * @param tag The tag to operate on.
		 * @return {@link MCReader} object or "null" if tag is "null" or tag is
		 * not Mifare Classic.
		 */
		public static MCReader get(Tag tag)
		{
			MCReader mcr = null;
			if (tag != null)
			{
				mcr = new MCReader(tag);
				if (!mcr.isMifareClassic())
				{
					return null;
				}
			}
			return mcr;
		}

		/**
		 * Read as much as possible from the tag with the given key information.
		 * @return A Key-Value Pair. Keys are the sector numbers, values are the tag data. This tag data (values) are arrays
		 * containing one block per field (index 0-3 or 0-15). If a block is "null" it means that the block couldn't be read
		 * with the given key information.<br />
		 * On Error "null" will be returned (most likely the tag was removed during reading). If none of the keys in the key
		 * map is valid for reading and therefore no sector is read, an empty set (SparseArray.size() == 0) will be
		 * returned.
		 * @see #buildNextKeyMapPart()
		 */

		public String[] readSector0(byte[] keyA) throws TagLostException
		{
			String[] ret = readSector(0, keyA, false);
			return ret;
		}

		public String[] readSector1(byte[] keyA) throws TagLostException
		{
			String[] ret = readSector(1, keyA, false);
			return ret;
		}

		/**
		 * Read as much as possible from the tag with the given key information.
		 * @param keyMap Keys (A and B) mapped to a sector. See {@link #buildNextKeyMapPart()}.
		 * @return A Key-Value Pair. Keys are the sector numbers, values are the tag data. This tag data (values) are arrays containing
		 * one block per field (index 0-3 or 0-15).
		 * If a block is "null" it means that the block couldn't be
		 * read with the given key information.<br />
		 * On Error, "null" will be returned (tag was removed during reading or
		 * keyMap is null). If none of the keys in the key map are valid for reading
		 * (and therefore no sector is read), an empty set (SparseArray.size() == 0)
		 * will be returned.
		 * @see #buildNextKeyMapPart()
		 */
		public SparseArray<String[]> readAsMuchAsPossible(SparseArray<byte[][]> keyMap)
		{
			SparseArray<String[]> resultSparseArray;
			if (keyMap != null && keyMap.size() > 0)
			{
				resultSparseArray = new SparseArray<String[]>(keyMap.size());
				// For all entries in map do:
				for (int i = 0; i < keyMap.size(); i++)
				{
					String[][] results = new String[2][];
					try
					{
						if (keyMap.valueAt(i)[0] != null)
						{
							// Read with key A.
							results[0] = readSector(keyMap.keyAt(i), keyMap.valueAt(i)[0], false);
						}
						if (keyMap.valueAt(i)[1] != null)
						{
							// Read with key B.
							results[1] = readSector(keyMap.keyAt(i), keyMap.valueAt(i)[1], true);
						}
					}
					catch (TagLostException e)
					{
						return null;
					}
					// Merge results.
					if (results[0] != null || results[1] != null)
					{
						resultSparseArray.put(keyMap.keyAt(i), mergeSectorData(results[0], results[1]));
					}
				}
				return resultSparseArray;
			}
			return null;
		}

		/**
		 * Read as much as possible from the tag depending on the
		 * mapping range and the given key information.
		 * The key information must be set before calling this method
		 * Also the mapping range must be specified before calling this method
		 * (use {@link #setMappingRange(int, int)}).
		 * Attention: This method builds a key map. Depending on the key count
		 * in the given key file, this could take more than a few minutes.
		 * The old key map from {@link #getKeyMap()} will be destroyed and
		 * the full new one is gettable afterwards.
		 * @return A Key-Value Pair. Keys are the sector numbers, values
		 * are the tag data. The tag data (values) are arrays containing
		 * one block per field (index 0-3 or 0-15).
		 * If a block is "null" it means that the block couldn't be
		 * read with the given key information.
		 * @see #buildNextKeyMapPart()
		 */
		public SparseArray<String[]> readAsMuchAsPossible()
		{
			mKeyMapStatus = getSectorCount();
			while (buildNextKeyMapPart() < getSectorCount() - 1)
				;
			return readAsMuchAsPossible(mKeyMap);
		}

		/**
		 * Read as much as possible from a sector with the given key.
		 * Best results are gained from a valid key B (except key B is marked as
		 * readable in the access conditions).
		 * @param sectorIndex Index of the Sector to read. (For Mifare Classic 1K:
		 * 0-63)
		 * @param key Key for authentication.
		 * @param useAsKeyB If true, key will be treated as key B
		 * for authentication.
		 * @return Array of blocks (index 0-3 or 0-15). If a block or a key is
		 * marked with {@link #NO_DATA} or {@link #NO_KEY}
		 * it means that this data could not be read or found. On authentication error
		 * "null" will be returned.
		 * @throws TagLostException When connection with/to tag is lost.
		 * @see #mergeSectorData(String[], String[])
		 */
		public String[] readSector(int sectorIndex, byte[] key, boolean useAsKeyB) throws TagLostException
		{
			boolean auth = authenticate(sectorIndex, key, useAsKeyB);
			String[] ret = null;
			// Read sector.
			if (auth)
			{
				// Read all blocks.
				ArrayList<String> blocks = new ArrayList<String>();
				int firstBlock = mMFC.sectorToBlock(sectorIndex);
				int lastBlock = firstBlock + 4;
				if (mMFC.getSize() == MifareClassic.SIZE_4K && sectorIndex > 31)
				{
					lastBlock = firstBlock + 16;
				}
				for (int i = firstBlock; i < lastBlock; i++)
				{
					try
					{
						byte blockBytes[] = mMFC.readBlock(i);
						// mMFC.readBlock(i) must return 16 bytes or throw an error.
						// At least this is what the documentation says.
						// On Samsung's Galaxy S5 and Sony's Xperia Z2 however, it
						// sometimes returns < 16 bytes for unknown reasons.
						// Update: Aaand sometimes it returns more than 16 bytes...
						// The appended byte(s) are 0x00.
						if (blockBytes.length < 16)
						{
							throw new IOException();
						}
						if (blockBytes.length > 16)
						{
							byte[] blockBytesTmp = Arrays.copyOf(blockBytes, 16);
							blockBytes = blockBytesTmp;
						}

						blocks.add(Common.byte2HexString(blockBytes));
					}
					catch (TagLostException e)
					{
						throw e;
					}
					catch (IOException e)
					{
						// Could not read block.
						// (Maybe due to key/authentication method.)
						Log.d(LOG_TAG, "(Recoverable) Error while reading block " + i + " from tag.");
						blocks.add(NO_DATA);
						if (!mMFC.isConnected())
						{
							throw new TagLostException("Tag removed during readSector(...)");
						}
						// After an error, a re-authentication is needed.
						authenticate(sectorIndex, key, useAsKeyB);
					}
				}
				ret = blocks.toArray(new String[blocks.size()]);
				int last = ret.length - 1;

				// Merge key in last block (sector trailer).
				if (!useAsKeyB)
				{
					if (isKeyBReadable(Common.hexStringToByteArray(ret[last].substring(12, 20))))
					{
						ret[last] = Common.byte2HexString(key) + ret[last].substring(12, 32);
					}
					else
					{
						ret[last] = Common.byte2HexString(key) + ret[last].substring(12, 20) + NO_KEY;
					}
				}
				else
				{
					if (ret[0].equals(NO_DATA))
					{
						// If Key B may be read in the corresponding Sector Trailer,
						// it cannot serve for authentication (according to NXP).
						// What they mean is that you can authenticate successfully,
						// but can not read data. In this case the
						// readBlock() result is 0 for each block.
						ret = null;
					}
					else
					{
						ret[last] = NO_KEY + ret[last].substring(12, 20) + Common.byte2HexString(key);
					}
				}
			}
			return ret;
		}

		public void decrement(int sector, int block, byte[] sectorKeyA, int value) throws IOException
		{
			boolean auth = authenticate(sector, sectorKeyA, false);
			if (auth)
			{
				int blockIndex = mMFC.sectorToBlock(sector) + block;
				mMFC.decrement(blockIndex, value);
				mMFC.transfer(blockIndex);
			}

		}

		public Integer toValueBlock(String blockHexData)
		{
			if (Common.isHexAnd16Byte(blockHexData) == false)
			{
				// Error. Not hex and 16 byte.
				return null;
			}

			if (Common.isValueBlock(blockHexData) == false)
			{
				// Error. No value block.
				return null;
			}

			// Decode.
			byte[] vbAsBytes = Common.hexStringToByteArray(blockHexData.substring(0, 8));
			// Bytes -> Int. -> reverse.
			int vbAsInt = Integer.reverseBytes(ByteBuffer.wrap(vbAsBytes).getInt());
			return vbAsInt;
		}

		/**
		 * Write a block of 16 byte data to tag.
		 * @param sectorIndex The sector to where the data should be written
		 * @param blockIndex The block to where the data should be written
		 * @param data 16 byte of data.
		 * @param key The Mifare Classic key for the given sector.
		 * @param useAsKeyB If true, key will be treated as key B
		 * for authentication.
		 * @return The return codes are:<br />
		 * <ul>
		 * <li>0 - Everything went fine.</li>
		 * <li>1 - Sector index is out of range.</li>
		 * <li>2 - Block index is out of range.</li>
		 * <li>3 - Data are not 16 bytes.</li>
		 * <li>4 - Authentication went wrong.</li>
		 * <li>-1 - Error while writing to tag.</li>
		 * </ul>
		 * @see #authenticate(int, byte[], boolean)
		 */
		public int writeBlock(int sectorIndex, int blockIndex, byte[] data, byte[] key, boolean useAsKeyB)
		{
			if (mMFC.getSectorCount() - 1 < sectorIndex)
			{
				return 1;
			}
			if (mMFC.getBlockCountInSector(sectorIndex) - 1 < blockIndex)
			{
				return 2;
			}
			if (data.length != 16)
			{
				return 3;
			}
			if (!authenticate(sectorIndex, key, useAsKeyB))
			{
				return 4;
			}
			// Write block.
			int block = mMFC.sectorToBlock(sectorIndex) + blockIndex;
			try
			{
				mMFC.writeBlock(block, data);
			}
			catch (IOException e)
			{
				Log.e(LOG_TAG, "Error while writing block to tag.", e);
				return -1;
			}
			return 0;
		}

		public void writeBlockExcept(int sectorIndex, int blockIndex, byte[] data, byte[] key, boolean useAsKeyB)
				throws IOException
		{
			int resultCode = writeBlock(sectorIndex, blockIndex, data, key, useAsKeyB);
			switch (resultCode)
			{
			case 0:
				break;
			case -1:
				throw new IOException("Error while writing to tag");
			case 1:
				throw new IOException("Sector index is out of range.");
			case 2:
				throw new IOException("Block index is out of range.");
			case 3:
				throw new IOException("Data is not 16 bytes long");
			default:
				break;
			}
		}

		/**
		 * Increase or decrease a Value Block.
		 * @param sectorIndex The sector to where the data should be written
		 * @param blockIndex The block to where the data should be written
		 * @param value Increase or decrease Value Block by this value.
		 * @param increment If true, increment Value Block by value. Decrement
		 * if false.
		 * @param key The Mifare Classic key for the given sector.
		 * @param useAsKeyB If true, key will be treated as key B
		 * for authentication.
		 * @return The return codes are:<br />
		 * <ul>
		 * <li>0 - Everything went fine.</li>
		 * <li>1 - Sector index is out of range.</li>
		 * <li>2 - Block index is out of range.</li>
		 * <li>3 - Authentication went wrong.</li>
		 * <li>-1 - Error while writing to tag.</li>
		 * </ul>
		 * @see #authenticate(int, byte[], boolean)
		 */
		public int writeValueBlock(int sectorIndex, int blockIndex, int value, boolean increment, byte[] key,
				boolean useAsKeyB)
		{
			if (mMFC.getSectorCount() - 1 < sectorIndex)
			{
				return 1;
			}
			if (mMFC.getBlockCountInSector(sectorIndex) - 1 < blockIndex)
			{
				return 2;
			}
			if (!authenticate(sectorIndex, key, useAsKeyB))
			{
				return 3;
			}
			// Write Value Block.
			int block = mMFC.sectorToBlock(sectorIndex) + blockIndex;
			try
			{
				if (increment)
				{
					mMFC.increment(block, value);
				}
				else
				{
					mMFC.decrement(block, value);
				}
				mMFC.transfer(block);
			}
			catch (IOException e)
			{
				Log.e(LOG_TAG, "Error while writing Value Block to tag.", e);
				return -1;
			}
			return 0;
		}

		/**
		 * Build Key-Value Pairs in which keys represent the sector and
		 * values are one or both of the Mifare keys (A/B).
		 * The Mifare key information must be set before calling this method
		 * Also the mapping range must be specified before calling this method
		 * (use {@link #setMappingRange(int, int)}).<br /><br />
		 * The mapping works like some kind of dictionary attack.
		 * All keys are checked against the next sector
		 * with both authentication methods (A/B). If at least one key was found
		 * for a sector, the map will be extended with an entry, containing the
		 * key(s) and the information for what sector the key(s) are. You can get
		 * this Key-Value Pairs by calling {@link #getKeyMap()}. A full
		 * key map can be gained by calling this method as often as there are
		 * sectors on the tag (See {@link #getSectorCount()}). If you call
		 * this method once more after a full key map was created, it resets the
		 * key map and starts all over.
		 * @return The sector that was just checked. On an error condition,
		 * it returns "-1" and resets the key map to "null".
		 * @see #getKeyMap()
		 * @see #setMappingRange(int, int)
		 * @see #readAsMuchAsPossible(SparseArray)
		 */
		public int buildNextKeyMapPart()
		{
			// Clear status and key map before new walk through sectors.
			boolean error = false;
			if (mKeysWithOrder != null && mLastSector != -1)
			{
				if (mKeyMapStatus == mLastSector + 1)
				{
					mKeyMapStatus = mFirstSector;
					mKeyMap = new SparseArray<byte[][]>();
				}

				// Get auto reconnect setting.
				boolean autoReconnect = false;//Common.getPreferences().getBoolean(Preference.AutoReconnect.toString(), false);

				byte[][] keys = new byte[2][];
				boolean[] foundKeys = new boolean[] {false, false};

				// Check next sector against all keys (lines) with
				// authentication method A and B.
				for (int i = 0; i < mKeysWithOrder.size();)
				{
					byte[] key = mKeysWithOrder.get(i);
					try
					{
						if (!foundKeys[0] && mMFC.authenticateSectorWithKeyA(mKeyMapStatus, key))
						{
							keys[0] = key;
							foundKeys[0] = true;
						}
						if (!foundKeys[1] && mMFC.authenticateSectorWithKeyB(mKeyMapStatus, key))
						{
							keys[1] = key;
							foundKeys[1] = true;
						}
					}
					catch (Exception e)
					{
						Log.d(LOG_TAG, "Error while building next key map part");
						// Is auto reconnect enabled?
						if (autoReconnect)
						{
							Log.d(LOG_TAG, "Auto reconnect is enabled");
							while (!isConnected())
							{
								// Sleep for 500ms.
								try
								{
									Thread.sleep(500);
								}
								catch (InterruptedException ex)
								{
									// Do nothing.
								}
								// Try to reconnect.
								try
								{
									connect();
								}
								catch (IOException ex)
								{
									// Do nothing.
								}
							}
							// Repeat last loop (do not incr. i).
							continue;
						}
						else
						{
							error = true;
							break;
						}
					}
					if (foundKeys[0] && foundKeys[1])
					{
						// Both keys found. Continue with next sector.
						break;
					}
					i++;
				}
				if (!error && (foundKeys[0] || foundKeys[1]))
				{
					// At least one key found. Add key(s).
					mKeyMap.put(mKeyMapStatus, keys);
					// Key reuse is very likely, so try these first
					// for the next sector.
					if (foundKeys[0])
					{
						mKeysWithOrder.remove(keys[0]);
						mKeysWithOrder.add(0, keys[0]);
					}
					if (foundKeys[1])
					{
						mKeysWithOrder.remove(keys[1]);
						mKeysWithOrder.add(0, keys[1]);
					}
				}
				mKeyMapStatus++;
			}
			else
			{
				error = true;
			}

			if (error)
			{
				mKeyMapStatus = 0;
				mKeyMap = null;
				return -1;
			}
			return mKeyMapStatus - 1;
		}

		/**
		 * Merge the result of two {@link #readSector(int, byte[], boolean)}
		 * calls on the same sector (with different keys or authentication methods).
		 * In this case merging means empty blocks will be overwritten with non
		 * empty ones and the keys will be added correctly to the sector trailer.
		 * The access conditions will be taken from the first (firstResult)
		 * parameter if it is not null.
		 * @param firstResult First
		 * {@link #readSector(int, byte[], boolean)} result.
		 * @param secondResult Second
		 * {@link #readSector(int, byte[], boolean)} result.
		 * @return Array (sector) as result of merging the given
		 * sectors. If a block is {@link #NO_DATA} it
		 * means that none of the given sectors contained data from this block.
		 * @see #readSector(int, byte[], boolean)
		 * @see #authenticate(int, byte[], boolean)
		 */
		public String[] mergeSectorData(String[] firstResult, String[] secondResult)
		{
			String[] ret = null;
			if (firstResult != null || secondResult != null)
			{
				if ((firstResult != null && secondResult != null) && firstResult.length != secondResult.length)
				{
					return null;
				}
				int length = (firstResult != null) ? firstResult.length : secondResult.length;
				ArrayList<String> blocks = new ArrayList<String>();
				// Merge data blocks.
				for (int i = 0; i < length - 1; i++)
				{
					if (firstResult != null && firstResult[i] != null && !firstResult[i].equals(NO_DATA))
					{
						blocks.add(firstResult[i]);
					}
					else if (secondResult != null && secondResult[i] != null && !secondResult[i].equals(NO_DATA))
					{
						blocks.add(secondResult[i]);
					}
					else
					{
						// None of the results got the data form the block.
						blocks.add(NO_DATA);
					}
				}
				ret = blocks.toArray(new String[blocks.size() + 1]);
				int last = length - 1;
				// Merge sector trailer.
				if (firstResult != null && firstResult[last] != null && !firstResult[last].equals(NO_DATA))
				{
					// Take first for sector trailer.
					ret[last] = firstResult[last];
					if (secondResult != null && secondResult[last] != null && !secondResult[last].equals(NO_DATA))
					{
						// Merge key form second result to sector trailer.
						ret[last] = ret[last].substring(0, 20) + secondResult[last].substring(20);
					}
				}
				else if (secondResult != null && secondResult[last] != null && !secondResult[last].equals(NO_DATA))
				{
					// No first result. Take second result as sector trailer.
					ret[last] = secondResult[last];
				}
				else
				{
					// No sector trailer at all.
					ret[last] = NO_DATA;
				}
			}
			return ret;
		}

		/**
		 * This method checks if the present tag is writable with the provided keys
		 * at the given positions (sectors, blocks). This is done by authenticating
		 * with one of the keys followed by reading and interpreting
		 * Access Conditions.
		 * @param pos A map of positions (key = sector, value = Array of blocks).
		 * For each of these positions you will get the write information
		 * (see return values).
		 * @param keyMap A key map generated by
		 * @return A map within a map (all with type = Integer).
		 * The key of the outer map is the sector number and the value is another
		 * map with key = block number and value = write information.
		 * The write information indicates which key is needed to write to the
		 * present tag at the given position.<br /><br />
		 * Write return codes are:<br />
		 * <ul>
		 * <li>0 - Never</li>
		 * <li>1 - Key A</li>
		 * <li>2 - Key B</li>
		 * <li>3 - Key A|B</li>
		 * <li>4 - Key A, but AC never</li>
		 * <li>5 - Key B, but AC never</li>
		 * <li>6 - Key B, but keys never</li>
		 * <li>-1 - Error</li>
		 * <li>Inner map == null - Whole sector is dead (IO Error) or ACs are
		 *  incorrect</li>
		 * <li>null - Authentication error</li>
		 * </ul>
		 */
		public HashMap<Integer, HashMap<Integer, Integer>> isWritableOnPositions(HashMap<Integer, int[]> pos,
				SparseArray<byte[][]> keyMap)
		{
			HashMap<Integer, HashMap<Integer, Integer>> ret = new HashMap<Integer, HashMap<Integer, Integer>>();
			for (int i = 0; i < keyMap.size(); i++)
			{
				int sector = keyMap.keyAt(i);
				if (pos.containsKey(sector))
				{
					byte[][] keys = keyMap.get(sector);
					byte[] ac;
					// Authenticate.
					if (keys[0] != null)
					{
						if (!authenticate(sector, keys[0], false))
						{
							return null;
						}
					}
					else if (keys[1] != null)
					{
						if (!authenticate(sector, keys[1], true))
						{
							return null;
						}
					}
					else
					{
						return null;
					}
					// Read Mifare Access Conditions.
					int acBlock = mMFC.sectorToBlock(sector) + mMFC.getBlockCountInSector(sector) - 1;
					try
					{
						ac = mMFC.readBlock(acBlock);
					}
					catch (Exception e)
					{
						ret.put(sector, null);
						continue;
					}
					// mMFC.readBlock(i) must return 16 bytes or throw an error.
					// At least this is what the documentation says.
					// On Samsung's Galaxy S5 and Sony's Xperia Z2 however, it
					// sometimes returns < 16 bytes for unknown reasons.
					// Update: Aaand sometimes it returns more than 16 bytes...
					// The appended byte(s) are 0x00.
					if (ac.length < 16)
					{
						ret.put(sector, null);
						continue;
					}

					ac = Arrays.copyOfRange(ac, 6, 9);
					byte[][] acMatrix = Common.acBytesToACMatrix(ac);
					if (acMatrix == null)
					{
						ret.put(sector, null);
						continue;
					}
					boolean isKeyBReadable = Common.isKeyBReadable(acMatrix[0][3], acMatrix[1][3], acMatrix[2][3]);

					// Check all Blocks with data (!= null).
					HashMap<Integer, Integer> blockWithWriteInfo = new HashMap<Integer, Integer>();
					for (int block : pos.get(sector))
					{
						if ((block == 3 && sector <= 31) || (block == 15 && sector >= 32))
						{
							// Sector Trailer.
							// Are the Access Bits writable?
							int acValue = Common.getOperationInfoForBlock(acMatrix[0][3], acMatrix[1][3],
									acMatrix[2][3], Common.Operations.WriteAC, true, isKeyBReadable);
							// Is key A writable? (If so, key B will be writable
							// with the same key.)
							int keyABValue = Common.getOperationInfoForBlock(acMatrix[0][3], acMatrix[1][3],
									acMatrix[2][3], Common.Operations.WriteKeyA, true, isKeyBReadable);

							int result = keyABValue;
							if (acValue == 0 && keyABValue != 0)
							{
								// Write key found, but AC-bits are not writable.
								result += 3;
							}
							else if (acValue == 2 && keyABValue == 0)
							{
								// Access Bits are writable with key B,
								// but keys are not writable.
								result = 6;
							}
							blockWithWriteInfo.put(block, result);
						}
						else
						{
							// Data block.
							int acBitsForBlock = block;
							// Handle Mifare Classic 4k Tags.
							if (sector >= 32)
							{
								if (block >= 0 && block <= 4)
								{
									acBitsForBlock = 0;
								}
								else if (block >= 5 && block <= 9)
								{
									acBitsForBlock = 1;
								}
								else if (block >= 10 && block <= 14)
								{
									acBitsForBlock = 2;
								}
							}
							blockWithWriteInfo.put(block, Common.getOperationInfoForBlock(acMatrix[0][acBitsForBlock],
									acMatrix[1][acBitsForBlock], acMatrix[2][acBitsForBlock], Common.Operations.Write,
									false, isKeyBReadable));
						}

					}
					if (blockWithWriteInfo.size() > 0)
					{
						ret.put(sector, blockWithWriteInfo);
					}
				}
			}
			return ret;
		}

		/**
		 * Set the key files for {@link #buildNextKeyMapPart()}.
		 * Key duplicates from the key file will be removed.
		 * @param keyFiles One or more key files.
		 * These files are simple text files with one key
		 * per line. Empty lines and lines STARTING with "#"
		 * will not be interpreted.
		 * will be shown.
		 * @return True if the key files are correctly loaded. False
		 * on error (out of memory).
		 */
		public boolean setKeyFile(File[] keyFiles)
		{
			HashSet<byte[]> keys = new HashSet<byte[]>();
			for (File file : keyFiles)
			{
				String[] lines = Common.readFileLineByLine(file, false);
				if (lines != null)
				{
					for (String line : lines)
					{
						if (!line.equals("") && line.length() == 12 && line.matches("[0-9A-Fa-f]+"))
						{
							try
							{
								keys.add(Common.hexStringToByteArray(line));
							}
							catch (OutOfMemoryError e)
							{
								// Error. Too many keys (out of memory).
								//Toast.makeText(context, R.string.info_to_many_keys, Toast.LENGTH_LONG).show();
								return false;
							}
						}
					}
				}
			}
			if (keys.size() > 0)
			{
				mKeysWithOrder = new ArrayList<byte[]>(keys);
			}
			return true;
		}

		/**
		 * Set the mapping range for {@link #buildNextKeyMapPart()}.
		 * @param firstSector Index of the first sector of the key map.
		 * @param lastSector Index of the last sector of the key map.
		 * @return True if range parameters were correct. False otherwise.
		 */
		public boolean setMappingRange(int firstSector, int lastSector)
		{
			if (firstSector >= 0 && lastSector < mMFC.getSectorCount() && firstSector <= lastSector)
			{
				mFirstSector = firstSector;
				mLastSector = lastSector;
				// Init. status of buildNextKeyMapPart to create a new key map.
				mKeyMapStatus = lastSector + 1;
				return true;
			}
			return false;
		}

		/**
		 * Authenticate with given sector of the tag.
		 * @param sectorIndex The sector with which to authenticate.
		 * @param key Key for the authentication.
		 * @param useAsKeyB If true, key will be treated as key B
		 * for authentication.
		 * @return True if authentication was successful. False otherwise.
		 */
		private boolean authenticate(int sectorIndex, byte[] key, boolean useAsKeyB)
		{
			try
			{
				if (!useAsKeyB)
				{
					// Key A.
					return mMFC.authenticateSectorWithKeyA(sectorIndex, key);
				}
				else
				{
					// Key B.
					return mMFC.authenticateSectorWithKeyB(sectorIndex, key);
				}
			}
			catch (IOException e)
			{
				Log.d(LOG_TAG, "Error authenticating with tag.");
			}
			return false;
		}

		/**
		 * Check if key B is readable.
		 * Key B is readable for the following configurations:
		 * <ul>
		 * <li>C1 = 0, C2 = 0, C3 = 0</li>
		 * <li>C1 = 0, C2 = 0, C3 = 1</li>
		 * <li>C1 = 0, C2 = 1, C3 = 0</li>
		 * </ul>
		 * @param ac The access conditions (4 bytes).
		 * @return True if key B is readable. False otherwise.
		 */
		private boolean isKeyBReadable(byte[] ac)
		{
			byte c1 = (byte)((ac[1] & 0x80) >>> 7);
			byte c2 = (byte)((ac[2] & 0x08) >>> 3);
			byte c3 = (byte)((ac[2] & 0x80) >>> 7);
			return c1 == 0 && (c2 == 0 && c3 == 0) || (c2 == 1 && c3 == 0) || (c2 == 0 && c3 == 1);
		}

		/**
		 * If you want a
		 * full key map, you have to call {@link #buildNextKeyMapPart()} as
		 * often as there are sectors on the tag
		 * (See {@link #getSectorCount()}).
		 * @return A Key-Value Pair. Keys are the sector numbers,
		 * values are the Mifare keys.
		 * The Mifare keys are 2D arrays with key type (first dimension, 0-1,
		 * 0 = KeyA / 1 = KeyB) and key (second dimension, 0-6). If a key is "null"
		 * it means that the key A or B (depending in the first dimension) could not
		 * be found.
		 * @see #getSectorCount()
		 * @see #buildNextKeyMapPart()
		 */
		public SparseArray<byte[][]> getKeyMap()
		{
			return mKeyMap;
		}

		public boolean isMifareClassic()
		{
			return mMFC != null;
		}

		/**
		 * Return the size of the Mifare Classic tag in bits.
		 * (e.g. Mifare Classic 1k = 1024)
		 * @return The size of the current tag.
		 */
		public int getSize()
		{
			return mMFC.getSize();
		}

		/**
		 * Return the sector count of the Mifare Classic tag.
		 * @return The sector count of the current tag.
		 */
		public int getSectorCount()
		{
			return mMFC.getSectorCount();
		}

		/**
		 * Return the block count of the Mifare Classic tag.
		 * @return The block count of the current tag.
		 */
		public int getBlockCount()
		{
			return mMFC.getBlockCount();
		}

		/**
		 * Return the block count in a specific sector.
		 * @param sectorIndex Index of a sector.
		 * @return Block count in given sector.
		 */
		public int getBlockCountInSector(int sectorIndex)
		{
			return mMFC.getBlockCountInSector(sectorIndex);
		}

		/**
		 * Check if the reader is connected to the tag.
		 * @return True if the reader is connected. False otherwise.
		 */
		public boolean isConnected()
		{
			return mMFC.isConnected();
		}

		/**
		 * Connect the reader to the tag.
		 */
		public void connect() throws IOException
		{
			try
			{
				mMFC.connect();
			}
			catch (IOException e)
			{
				Log.d(LOG_TAG, "Error while connecting to tag.");
				throw e;
			}
		}

		/**
		 * Close the connection between reader and tag.
		 */
		public void close()
		{
			try
			{
				mMFC.close();
			}
			catch (IOException e)
			{
				Log.d(LOG_TAG, "Error on closing tag.");
			}
		}
	}

	/**
	 * Common functions and variables for all Activities.
	 * @author Gerhard Klostermeier
	 */
	public static class Common
	{

		/**
		 * The directory name of the root directory of this app (on external storage).
		 */
		public static final String HOME_DIR = "/MifareClassicTool";
		/**
		 * The directory name of the key files directory (sub directory of {@link #HOME_DIR}.)
		 */
		public static final String KEYS_DIR = "/key-files";
		/**
		 * The directory name of the dump files directory (sub directory of {@link #HOME_DIR}.)
		 */
		public static final String DUMPS_DIR = "/dump-files";
		/**
		 * This file contains some standard Mifare keys.
		 * <ul>
		 * <li>0xFFFFFFFFFFFF - Unformatted, factory fresh tags.</li>
		 * <li>0xA0A1A2A3A4A5 - First sector of the tag (Mifare MAD).</li>
		 * <li>0xD3F7D3F7D3F7 - All other sectors.</li>
		 * <li>Others from {@link #SOME_CLASSICAL_KNOWN_KEYS}.</li>
		 * </ul>
		 */
		public static final String STD_KEYS = "std.keys";

		/**
		 * Some classical Mifare keys retrieved by a quick google search ("mifare standard keys").
		 */
		public static final String[] SOME_CLASSICAL_KNOWN_KEYS = {"000000000000", "A0B0C0D0E0F0", "A1B1C1D1E1F1",
				"B0B1B2B3B4B5", "4D3A99C351DD", "1A982C7E459A", "AABBCCDDEEFF"};

		/**
		 * Possible operations the on a Mifare Classic Tag.
		 */
		public enum Operations
		{
			Read, Write, Increment, DecTransRest, ReadKeyA, ReadKeyB, ReadAC, WriteKeyA, WriteKeyB, WriteAC
		}

		private static final String LOG_TAG = Common.class.getSimpleName();

		/**
		 * The last detected tag. Set by {@link #treatAsNewTag(Intent, Context)}
		 */
		private static Tag mTag = null;
		/**
		 * The last detected UID. Set by {@link #treatAsNewTag(Intent, Context)}
		 */
		private static byte[] mUID = null;
		/**
		 * Just a global storage to save key maps generated by
		 * @see MCReader#getKeyMap()
		 */
		private static SparseArray<byte[][]> mKeyMap = null;

		private static NfcAdapter mNfcAdapter;

		/**
		 * Check if a (hex) string is pure hex (0-9, A-F, a-f) and 16 byte
		 * (32 chars) long. If not show an error Toast in the context.
		 * @param hexString The string to check.
		 * @return True if sting is hex an 16 Bytes long, False otherwise.
		 */
		public static boolean isHexAnd16Byte(String hexString)
		{
			if (hexString.matches("[0-9A-Fa-f]+") == false)
			{
				// Error, not hex.
				return false;
			}
			if (hexString.length() != 32)
			{
				// Error, not 16 byte (32 chars).
				return false;
			}
			return true;
		}

		/**
		 * Check if the given block (hex string) is a value block.
		 * NXP has PDFs describing what value blocks are. Google something
		 * like "nxp mifare classic value block" if you want to have a
		 * closer look.
		 * @param hexString Block data as hex string.
		 * @return True if it is a value block. False otherwise.
		 */
		public static boolean isValueBlock(String hexString)
		{
			byte[] b = Common.hexStringToByteArray(hexString);
			if (b.length == 16)
			{
				// Google some NXP info PDFs about Mifare Classic to see how
				// Value Blocks are formated.
				// For better reading (~ = invert operator):
				// if (b0=b8 and b0=~b4) and (b1=b9 and b9=~b5) ...
				// ... and (b12=b14 and b13=b15 and b12=~b13) then
				if ((b[0] == b[8] && (byte)(b[0] ^ 0xFF) == b[4]) && (b[1] == b[9] && (byte)(b[1] ^ 0xFF) == b[5])
						&& (b[2] == b[10] && (byte)(b[2] ^ 0xFF) == b[6])
						&& (b[3] == b[11] && (byte)(b[3] ^ 0xFF) == b[7])
						&& (b[12] == b[14] && b[13] == b[15] && (byte)(b[12] ^ 0xFF) == b[13]))
				{
					return true;
				}
			}
			return false;
		}

		/**
		 * Read a file line by line. The file should be a simple text file. Empty lines and lines STARTING with "#" will not
		 * be interpreted.
		 * @param file The file to read.
		 * @param readComments Whether to read comments or to ignore them. Comments are lines STARTING with "#" (and empty
		 * lines).
		 * @return Array of strings representing the lines of the file. If the file is empty or an error occurs "null" will
		 * be returned.
		 */
		public static String[] readFileLineByLine(File file, boolean readComments)
		{
			BufferedReader br = null;
			String[] ret = null;
			if (file != null && file.exists())
			{
				try
				{
					br = new BufferedReader(new FileReader(file));

					String line;
					ArrayList<String> linesArray = new ArrayList<String>();
					while ((line = br.readLine()) != null)
					{
						// Ignore empty an comment lines.
						if (readComments || (!line.equals("") && !line.startsWith("#")))
						{
							linesArray.add(line);
						}
					}
					if (linesArray.size() > 0)
					{
						ret = linesArray.toArray(new String[linesArray.size()]);
					}
					else
					{
						ret = new String[] {""};
					}
				}
				catch (Exception e)
				{
					Log.e(LOG_TAG, "Error while reading from file " + file.getPath() + ".", e);
					ret = null;
				}
				finally
				{
					if (br != null)
					{
						try
						{
							br.close();
						}
						catch (IOException e)
						{
							Log.e(LOG_TAG, "Error while closing file.", e);
							ret = null;
						}
					}
				}
			}
			return ret;
		}

		/**
		 * Write an array of strings (each field is one line) to a given file. If the file already exists, it will be
		 * overwritten.
		 * @param file The file to write to.
		 * @param lines The lines to save.
		 * @return True if file writing was successful. False otherwise.
		 */
		public static boolean saveFile(File file, String[] lines)
		{
			boolean noError = true;
			if (file != null)
			{
				BufferedWriter bw = null;
				try
				{
					bw = new BufferedWriter(new FileWriter(file));
					int i;
					for (i = 0; i < lines.length - 1; i++)
					{
						bw.write(lines[i]);
						bw.newLine();
					}
					bw.write(lines[i]);
				}
				catch (IOException e)
				{
					Log.e(LOG_TAG, "Error while writing to '" + file.getName() + "' file.", e);
					noError = false;

				}
				finally
				{
					if (bw != null)
					{
						try
						{
							bw.close();
						}
						catch (IOException e)
						{
							Log.e(LOG_TAG, "Error while closing file.", e);
							noError = false;
						}
					}
				}
			}
			else
			{
				noError = false;
			}
			return noError;
		}

		/**
		 * Enables the NFC foreground dispatch system for the given Activity.
		 * @param targetActivity The Activity that is in foreground and wants to have NFC Intents.
		 * @see #disableNfcForegroundDispatch(Activity)
		 */
		public static void enableNfcForegroundDispatch(Activity targetActivity)
		{
			if (mNfcAdapter != null && mNfcAdapter.isEnabled())
			{

				Intent intent = new Intent(targetActivity, targetActivity.getClass())
						.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
				PendingIntent pendingIntent = PendingIntent.getActivity(targetActivity, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
				mNfcAdapter.enableForegroundDispatch(targetActivity, pendingIntent, null,
						new String[][] {new String[] {NfcA.class.getName()}});
			}
		}

		/**
		 * Disable the NFC foreground dispatch system for the given Activity.
		 * @param targetActivity An Activity that is in foreground and has NFC foreground dispatch system enabled.
		 * @see #enableNfcForegroundDispatch(Activity)
		 */
		public static void disableNfcForegroundDispatch(Activity targetActivity)
		{
			if (mNfcAdapter != null && mNfcAdapter.isEnabled())
			{
				mNfcAdapter.disableForegroundDispatch(targetActivity);
			}
		}

		/**
		 * For Activities which want to treat new Intents as Intents with a new Tag attached. If the given Intent has a Tag
		 * extra, the {@link #mTag} and {@link #mUID} will be updated and a Toast message will be shown in the calling
		 * Context (Activity). This method will also check if the device/tag supports Mifare Classic (see return values).
		 * @param intent The Intent which should be checked for a new Tag.
		 * @param context The Context in which the Toast will be shown.
		 * @return <ul>
		 * <li>1 - The device/tag supports Mifare Classic</li>
		 * <li>0 - The device/tag does not support Mifare Classic</li>
		 * <li>-1 - Wrong Intent (action is not "ACTION_TECH_DISCOVERED").</li>
		 * </ul>
		 * @see #mTag
		 * @see #mUID
		 */
		public static int treatAsNewTag(Intent intent, Context context)
		{
			// Check if Intent has a NFC Tag.
			if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(intent.getAction()))
			{
				Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
				mTag = tag;
				mUID = tag.getId();

				// Show Toast message with UID.
				/*String id = context.getResources().getString(
				        R.string.info_new_tag_found) + " (UID: ";
				id += byte2HexString(tag.getId());
				id += ")";
				Toast.makeText(context, id, Toast.LENGTH_LONG).show();*/

				// Return "1" if device supports Mifare Classic. "0" otherwise.
				return (Arrays.asList(tag.getTechList()).contains(MifareClassic.class.getName())) ? 1 : 0;
			}
			return -1;
		}

		/**
		 * Create a connected {@link MCReader} if there is a present Mifare Classic tag. If there is no Mifare Classic tag a
		 * error message will be displayed to toe user.
		 * @param context The Context in which the error Toast will be shown.
		 * @return A connected {@link MCReader} or "null" if no tag was present.
		 * @throws IOException 
		 */
		public static MCReader checkForTagAndCreateReader(Context context) throws IOException
		{
			// Check for tag.
			if (mTag == null)
			{
				// Error. There is no tag.
				//            Toast.makeText(context, R.string.info_no_tag_found,
				//                    Toast.LENGTH_LONG).show();
				return null;
			}
			MCReader reader = MCReader.get(mTag);
			if (reader == null)
			{
				// Error. The tag is not Mifare Classic.
				//            Toast.makeText(context, R.string.info_no_tag_found,
				//                    Toast.LENGTH_LONG).show();
				return null;
			}
			reader.connect();
			if (!reader.isConnected())
			{
				// Error. The tag is gone.
				//            Toast.makeText(context, R.string.info_no_tag_found,
				//                    Toast.LENGTH_LONG).show();
				reader.close();
				return null;
			}
			return reader;
		}

		/**
		 * Depending on the provided Access Conditions this method will return with which key you can achieve the operation
		 * ({@link Operations}) you asked for.<br />
		 * This method contains the table from the NXP Mifare Classic Datasheet.
		 * @param c1 Access Condition byte "C!".
		 * @param c2 Access Condition byte "C2".
		 * @param c3 Access Condition byte "C3".
		 * @param op The operation you want to do.
		 * @param isSectorTrailer True if it is a Sector Trailer, False otherwise.
		 * @param isKeyBReadable True if key B is readable, False otherwise.
		 * @return The operation "op" is possible with:<br />
		 * <ul>
		 * <li>0 - Never.</li>
		 * <li>1 - Key A.</li>
		 * <li>2 - Key B.</li>
		 * <li>3 - Key A or B.</li>
		 * <li>-1 - Error.</li>
		 * </ul>
		 */
		public static int getOperationInfoForBlock(byte c1, byte c2, byte c3, Operations op, boolean isSectorTrailer,
				boolean isKeyBReadable)
		{
			// Is Sector Trailer?
			if (isSectorTrailer)
			{
				// Sector Trailer.
				if (op != Operations.ReadKeyA && op != Operations.ReadKeyB && op != Operations.ReadAC
						&& op != Operations.WriteKeyA && op != Operations.WriteKeyB && op != Operations.WriteAC)
				{
					// Error. Sector Trailer but no Sector Trailer permissions.
					return 4;
				}
				if (c1 == 0 && c2 == 0 && c3 == 0)
				{
					if (op == Operations.WriteKeyA || op == Operations.WriteKeyB || op == Operations.ReadKeyB
							|| op == Operations.ReadAC)
					{
						return 1;
					}
					return 0;
				}
				else if (c1 == 0 && c2 == 1 && c3 == 0)
				{
					if (op == Operations.ReadKeyB || op == Operations.ReadAC)
					{
						return 1;
					}
					return 0;
				}
				else if (c1 == 1 && c2 == 0 && c3 == 0)
				{
					if (op == Operations.WriteKeyA || op == Operations.WriteKeyB)
					{
						return 2;
					}
					if (op == Operations.ReadAC)
					{
						return 3;
					}
					return 0;
				}
				else if (c1 == 1 && c2 == 1 && c3 == 0)
				{
					if (op == Operations.ReadAC)
					{
						return 3;
					}
					return 0;
				}
				else if (c1 == 0 && c2 == 0 && c3 == 1)
				{
					if (op == Operations.ReadKeyA)
					{
						return 0;
					}
					return 1;
				}
				else if (c1 == 0 && c2 == 1 && c3 == 1)
				{
					if (op == Operations.ReadAC)
					{
						return 3;
					}
					if (op == Operations.ReadKeyA || op == Operations.ReadKeyB)
					{
						return 0;
					}
					return 2;
				}
				else if (c1 == 1 && c2 == 0 && c3 == 1)
				{
					if (op == Operations.ReadAC)
					{
						return 3;
					}
					if (op == Operations.WriteAC)
					{
						return 2;
					}
					return 0;
				}
				else if (c1 == 1 && c2 == 1 && c3 == 1)
				{
					if (op == Operations.ReadAC)
					{
						return 3;
					}
					return 0;
				}
				else
				{
					return -1;
				}
			}
			else
			{
				// Data Block.
				if (op != Operations.Read && op != Operations.Write && op != Operations.Increment
						&& op != Operations.DecTransRest)
				{
					// Error. Data block but no data block permissions.
					return -1;
				}
				if (c1 == 0 && c2 == 0 && c3 == 0)
				{
					return (isKeyBReadable) ? 1 : 3;
				}
				else if (c1 == 0 && c2 == 1 && c3 == 0)
				{
					if (op == Operations.Read)
					{
						return (isKeyBReadable) ? 1 : 3;
					}
					return 0;
				}
				else if (c1 == 1 && c2 == 0 && c3 == 0)
				{
					if (op == Operations.Read)
					{
						return (isKeyBReadable) ? 1 : 3;
					}
					if (op == Operations.Write)
					{
						return 2;
					}
					return 0;
				}
				else if (c1 == 1 && c2 == 1 && c3 == 0)
				{
					if (op == Operations.Read || op == Operations.DecTransRest)
					{
						return (isKeyBReadable) ? 1 : 3;
					}
					return 2;
				}
				else if (c1 == 0 && c2 == 0 && c3 == 1)
				{
					if (op == Operations.Read || op == Operations.DecTransRest)
					{
						return (isKeyBReadable) ? 1 : 3;
					}
					return 0;
				}
				else if (c1 == 0 && c2 == 1 && c3 == 1)
				{
					if (op == Operations.Read || op == Operations.Write)
					{
						return 2;
					}
					return 0;
				}
				else if (c1 == 1 && c2 == 0 && c3 == 1)
				{
					if (op == Operations.Read)
					{
						return 2;
					}
					return 0;
				}
				else if (c1 == 1 && c2 == 1 && c3 == 1)
				{
					return 0;
				}
				else
				{
					// Error.
					return -1;
				}
			}
		}

		/**
		 * Check if key B is readable. Key B is readable for the following configurations:
		 * <ul>
		 * <li>C1 = 0, C2 = 0, C3 = 0</li>
		 * <li>C1 = 0, C2 = 0, C3 = 1</li>
		 * <li>C1 = 0, C2 = 1, C3 = 0</li>
		 * </ul>
		 * @param c1 Access Condition byte "C1"
		 * @param c2 Access Condition byte "C2"
		 * @param c3 Access Condition byte "C3"
		 * @return True if key B is readable. False otherwise.
		 */
		public static boolean isKeyBReadable(byte c1, byte c2, byte c3)
		{
			if (c1 == 0 && (c2 == 0 && c3 == 0) || (c2 == 1 && c3 == 0) || (c2 == 0 && c3 == 1))
			{
				return true;
			}
			return false;
		}

		/**
		* Convert the Access Condition bytes to a matrix containing the
		* resolved C1, C2 and C3 for each block.
		* @param acBytes The Access Condition bytes (3 byte).
		* @return Matrix of access conditions bits (C1-C3) where the first
		* dimension is the "C" parameter (C1-C3, Index 0-2) and the second
		* dimension is the block number (Index 0-3). If the ACs are incorrect
		* null will be returned.
		*/
		public static byte[][] acBytesToACMatrix(byte acBytes[])
		{
			// ACs correct?
			// C1 (Byte 7, 4-7) == ~C1 (Byte 6, 0-3) and
			// C2 (Byte 8, 0-3) == ~C2 (Byte 6, 4-7) and
			// C3 (Byte 8, 4-7) == ~C3 (Byte 7, 0-3)
			byte[][] acMatrix = new byte[3][4];
			if (acBytes.length > 2 && (byte)((acBytes[1] >>> 4) & 0x0F) == (byte)((acBytes[0] ^ 0xFF) & 0x0F)
					&& (byte)(acBytes[2] & 0x0F) == (byte)(((acBytes[0] ^ 0xFF) >>> 4) & 0x0F)
					&& (byte)((acBytes[2] >>> 4) & 0x0F) == (byte)((acBytes[1] ^ 0xFF) & 0x0F))
			{
				// C1, Block 0-3
				for (int i = 0; i < 4; i++)
				{
					acMatrix[0][i] = (byte)((acBytes[1] >>> 4 + i) & 0x01);
				}
				// C2, Block 0-3
				for (int i = 0; i < 4; i++)
				{
					acMatrix[1][i] = (byte)((acBytes[2] >>> i) & 0x01);
				}
				// C3, Block 0-3
				for (int i = 0; i < 4; i++)
				{
					acMatrix[2][i] = (byte)((acBytes[2] >>> 4 + i) & 0x01);
				}
				return acMatrix;
			}
			return null;
		}

		/**
		 * Convert the Access Condition bytes to a matrix containing the resolved C1, C2 and C3 for each block.
		 * @param ac The Access Conditions.
		 * @return Matrix of access conditions bits (C1-C3) where the first dimension is the "C" parameter (C1-C3, Index
		 * 0-2) and the second dimension is the block number (Index 0-3).
		 */
		public static byte[][] acToACMatrix(byte ac[])
		{
			// ACs correct?
			// C1 (Byte 7, 4-7) == ~C1 (Byte 6, 0-3) and
			// C2 (Byte 8, 0-3) == ~C2 (Byte 6, 4-7) and
			// C3 (Byte 8, 4-7) == ~C3 (Byte 7, 0-3)
			byte[][] acMatrix = new byte[3][4];
			if ((byte)((ac[1] >>> 4) & 0x0F) == (byte)((ac[0] ^ 0xFF) & 0x0F)
					&& (byte)(ac[2] & 0x0F) == (byte)(((ac[0] ^ 0xFF) >>> 4) & 0x0F)
					&& (byte)((ac[2] >>> 4) & 0x0F) == (byte)((ac[1] ^ 0xFF) & 0x0F))
			{
				// C1, Block 0-3
				for (int i = 0; i < 4; i++)
				{
					acMatrix[0][i] = (byte)((ac[1] >>> 4 + i) & 0x01);
				}
				// C2, Block 0-3
				for (int i = 0; i < 4; i++)
				{
					acMatrix[1][i] = (byte)((ac[2] >>> i) & 0x01);
				}
				// C3, Block 0-3
				for (int i = 0; i < 4; i++)
				{
					acMatrix[2][i] = (byte)((ac[2] >>> 4 + i) & 0x01);
				}
				return acMatrix;
			}
			return null;
		}

		/**
		 * Convert an array of bytes into a string of hex values.
		 * @param bytes Bytes to convert.
		 * @return The bytes in hex string format.
		 */
		public static String byte2HexString(byte[] bytes)
		{
			String ret = "";
			for (Byte b : bytes)
			{
				ret += String.format("%02X", b.intValue() & 0xFF);
			}
			return ret;
		}

		/**
		 * Convert a string of hex data into a byte array. Original author is: Dave L. (http://stackoverflow.com/a/140861).
		 * @param s The hex string to convert
		 * @return An array of bytes with the values of the string.
		 */
		public static byte[] hexStringToByteArray(String s)
		{
			int len = s.length();
			byte[] data = new byte[len / 2];
			try
			{
				for (int i = 0; i < len; i += 2)
				{
					data[i / 2] = (byte)((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i + 1), 16));
				}
			}
			catch (Exception e)
			{
				Log.d(LOG_TAG, "Argument(s) for hexStringToByteArray(String s)" + "was not a hex string");
			}
			return data;
		}

		/**
		 * Create a colored string.
		 * @param data The text to be colored.
		 * @param color The color for the text.
		 * @return A colored string.
		 */
		public static SpannableString colorString(String data, int color)
		{
			SpannableString ret = new SpannableString(data);
			ret.setSpan(new ForegroundColorSpan(color), 0, data.length(), 0);
			return ret;
		}

		/**
		 * Get the current active (last detected) Tag.
		 * @return The current active Tag.
		 * @see #mTag
		 */
		public static Tag getTag()
		{
			return mTag;
		}

		/**
		 * Set the new active Tag.
		 * @param tag The new Tag.
		 */
		public static void setTag(Tag tag)
		{
			mTag = tag;
		}

		/**
		 * Get the App wide used NFC adapter.
		 * @return NFC adapter.
		 */
		public static NfcAdapter getNfcAdapter()
		{
			return mNfcAdapter;
		}

		/**
		 * Set the App wide used NFC adapter.
		 * @param nfcAdapter The NFC adapter that should be used.
		 */
		public static void setNfcAdapter(NfcAdapter nfcAdapter)
		{
			mNfcAdapter = nfcAdapter;
		}

		/**
		 * @return A key map (see {@link MCReader#getKeyMap()}).
		 */
		public static SparseArray<byte[][]> getKeyMap()
		{
			return mKeyMap;
		}

		/**
		 * Set the key map.
		 * @param value A key map (see {@link MCReader#getKeyMap()}).
		 */
		public static void setKeyMap(SparseArray<byte[][]> value)
		{
			mKeyMap = value;
		}

		/**
		 * Get the UID of the current tag.
		 * @return The UID of the current tag.
		 * @see #mUID
		 */
		public static byte[] getUID()
		{
			return mUID;
		}
	}

	public static abstract class NFCReceiver
	{
		private Intent mOldIntent = null;
		private boolean mResume = true;
		private Activity activity;

		public NFCReceiver(Activity activity)
		{
			this.activity = activity;
			activity.getApplication().registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks()
			{
				@Override
				public void onActivityStopped(Activity activity)
				{
				}

				@Override
				public void onActivityStarted(Activity activity)
				{
				}

				@Override
				public void onActivitySaveInstanceState(Activity activity, Bundle outState)
				{
				}

				@Override
				public void onActivityResumed(Activity activity)
				{
					if (activity == NFCReceiver.this.activity)
						onActivityResume();
				}

				@Override
				public void onActivityPaused(Activity activity)
				{
					if (activity == NFCReceiver.this.activity)
						onActivityPause();
				}

				@Override
				public void onActivityDestroyed(Activity activity)
				{
				}

				@Override
				public void onActivityCreated(Activity activity, Bundle savedInstanceState)
				{
				}
			});

			initializeNFC(true);
		}

		private void initializeNFC(boolean isNfcOptional)
		{
			if (Common.getNfcAdapter() == null)
			{
				Common.setNfcAdapter(NfcAdapter.getDefaultAdapter(activity));
				if (Common.getNfcAdapter() == null && !isNfcOptional)
				{
					new AlertDialog.Builder(activity).setTitle(R.string.dialog_no_nfc_title)
							.setMessage(R.string.dialog_no_nfc).setIcon(android.R.drawable.ic_dialog_alert)
							.setPositiveButton(R.string.button_exit, new DialogInterface.OnClickListener()
							{
								public void onClick(DialogInterface dialog, int which)
								{
									activity.finish();
								}
							}).setOnCancelListener(new DialogInterface.OnCancelListener()
							{
								public void onCancel(DialogInterface dialog)
								{
									activity.finish();
								}
							}).show();
					mResume = false;
					return;
				}
			}
		}

		private void askForActivationOfNFC()
		{
			// Create a dialog that send user to NFC settings if NFC is off.
			// (Or let the user use the App in editor only mode / exit the App.)	
			final int negativeButtonTextID = R.string.button_exit;

			new AlertDialog.Builder(activity) //
					.setTitle(R.string.dialog_nfc_not_enabled_title) //
					.setMessage(R.string.dialog_nfc_not_enabled) //
					.setIcon(android.R.drawable.ic_dialog_info) //
					.setPositiveButton(R.string.button_nfc, new DialogInterface.OnClickListener()
					{
						@SuppressLint("InlinedApi")
						public void onClick(DialogInterface dialog, int which)
						{
							// Goto NFC Settings.
							if (Build.VERSION.SDK_INT >= 16)
							{
								activity.startActivity(new Intent(Settings.ACTION_NFC_SETTINGS));
							}
							else
							{
								activity.startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS));
							}
						}
					}).setNegativeButton(negativeButtonTextID, new DialogInterface.OnClickListener()
					{
						public void onClick(DialogInterface dialog, int id)
						{
							// Exit the App.
							activity.finish();
						}
					}) //
					.show();
		}

		/**
		 * Check if NFC adapter is enabled. If not, show the user a dialog and let him choose between "Goto NFC Setting",
		 * "Use Editor Only" and "Exit App". Also enable NFC foreground dispatch system.
		 * @see Common#enableNfcForegroundDispatch(Activity)
		 */
		private void checkNfc()
		{
			// Check if the NFC hardware is enabled.
			if (Common.getNfcAdapter() != null && !Common.getNfcAdapter().isEnabled())
			{
				// NFC is disabled. Show dialog.
				askForActivationOfNFC();
				return;
			}
			else if (Common.getNfcAdapter() != null)
			{
				// NFC is enabled. Hide dialog and enable NFC
				// foreground dispatch.
				if (mOldIntent != activity.getIntent())
				{
					readTag(activity.getIntent());
					mOldIntent = activity.getIntent();
				}
				Common.enableNfcForegroundDispatch(activity);
				//mEnableNfc.hide();
			}
		}

		public void readTag(Intent intent)
		{
			int result = Common.treatAsNewTag(intent, activity);
			if (result < 0)
			{
				return; //Intent is not a tag read intent
			}
			else if (result == 0)
			{
				//VahapT: Even if the device supports Mifare Classic, if two cards are simultaneously enters into the range of the device then 
				showInvalidTagMessage(activity.getString(R.string.card_not_supported));
			}
			else
			{
				MCReader reader = null;
				try
				{
					reader = Common.checkForTagAndCreateReader(activity);
					if (reader == null)
						throw new TagLostException(activity.getString(R.string.read_failed));

					String cardUID = Common.byte2HexString(Common.getUID());
					if ("1520A75F".equals(cardUID)) //VTODO: for testing only, delete this
						cardUID = "0462C65A8B3380";
					else if ("84B45F91".equals(cardUID))
						cardUID = "0462C65A8B3380";
					else if ("14536891".equals(cardUID))
						cardUID = "0462C65A8B3380";
					onCardRead(cardUID, reader);
				}
				catch (UnsupportedEncodingException e)
				{
					handleException(e);
				}
				catch (RuntimeException e)
				{
					handleException(e);
				}
				catch (TagLostException e)
				{
					handleException(e);
				}
				catch (IOException e)
				{
					handleException(e);
				}
				catch (Exception e)
				{
					handleException(e);
				}
				finally
				{
					if (reader != null)
						reader.close();
				}
			}
		}

		protected abstract void onCardRead(String chipID, MCReader reader) throws IOException, IllegalArgumentException;

		public void handleException(Exception e)
		{
			if (e instanceof UnsupportedEncodingException)
				showUnexpectedErrorMessage(e);
			else if (e instanceof RuntimeException)
				showUnexpectedErrorMessage(e);
			else if (e instanceof TagLostException)
				showErrorMessage(e.getMessage());
			else if (e instanceof IOException)
				showErrorMessage(e.getMessage());
			else
				showUnexpectedErrorMessage(e);
		}

		private void showUnexpectedErrorMessage(Exception e)
		{
			String id = activity.getResources().getString(R.string.info_unexpected_error_occured) + e.getMessage();
			displayErrorMessage(activity, R.string.dialog_title_generic_error, id);
		}

		private void showErrorMessage(String message)
		{
			//StateRepository.getInstance().setApprovalResult(ApprovalResult.READ_FAILED);
			//showActivity(MifareReaderActivity.class);
			displayErrorMessage(activity, R.string.dialog_title_generic_error, message);
		}

		private void showInvalidTagMessage(String message)
		{
			//StateRepository.getInstance().setApprovalResult(ApprovalResult.TAG_INVALID);
			//showActivity(MifareReaderActivity.class);
			//		// Show Toast message with UID.
			//		String msgId = this.getResources().getString(R.string.info_tag_invalid);
			//Toast.makeText(activity, message, Toast.LENGTH_LONG).show();
			displayErrorMessage(activity, R.string.dialog_title_generic_error, message);
		}

		/**
		 * Enable NFC foreground dispatch system.
		 * @see Common#disableNfcForegroundDispatch(Activity)
		 */
		private void onActivityResume()
		{
			if (mResume)
				checkNfc();
		}

		/**
		 * Disable NFC foreground dispatch system.
		 * @see Common#disableNfcForegroundDispatch(Activity)
		 */
		private void onActivityPause()
		{
			Common.disableNfcForegroundDispatch(activity);
		}

		public static byte[] calculateKeyA(String chipId, int sectorNum) throws IOException
		{
			try
			{
				chipId = chipId.substring(0, 8);
				chipId = "redrose_fsdFG_dflj943" + chipId;
				byte[] partA = chipId.getBytes(Charset.forName("ASCII"));
				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				bos.write(partA);
				bos.write(sectorNum);
				bos.write("redrose_nvl0n".getBytes(Charset.forName("ASCII")));

				byte[] chipIdBytes = bos.toByteArray();
				MessageDigest md = MessageDigest.getInstance("SHA");
				md.update(chipIdBytes, 0, chipIdBytes.length);
				byte[] hash = md.digest();
				byte[] keyA = new byte[6];
				System.arraycopy(hash, 0, keyA, 0, 6);
				return keyA;
			}
			catch (NoSuchAlgorithmException e)
			{
				throw new RuntimeException(e);
			}
		}

		public static byte[] calculateKeyB(String chipId, int sectorNum) throws IOException
		{
			try
			{
				String keyStart = "redrose_dawe0ew2.234g";
				String keyEnd = "redrose_s.ser";
				if (sectorNum > 13)
					sectorNum = 13;
				else if (sectorNum < 2)
					sectorNum = -1;
				else
					throw new IllegalArgumentException("Invalid sector " + sectorNum);

				chipId = chipId.substring(0, 8);
				chipId = keyStart + chipId;
				byte[] partA = chipId.getBytes(Charset.forName("ASCII"));
				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				bos.write(partA);
				if (sectorNum >= 0)
					bos.write(sectorNum);
				bos.write(keyEnd.getBytes(Charset.forName("ASCII")));

				byte[] chipIdBytes = bos.toByteArray();
				MessageDigest md = MessageDigest.getInstance("SHA");
				md.update(chipIdBytes, 0, chipIdBytes.length);
				byte[] hash = md.digest();
				byte[] keyA = new byte[6];
				System.arraycopy(hash, 0, keyA, 0, 6);
				return keyA;
			}
			catch (NoSuchAlgorithmException e)
			{
				throw new RuntimeException(e);
			}
		}

		public static int readCardVersion(String[] sector1)
		{
			byte v = Common.hexStringToByteArray(sector1[2].substring(0, 2))[0];
			return v;
		}

		public static String readChipIdExtension(String[] sector1) throws UnsupportedEncodingException
		{
			return new String(Common.hexStringToByteArray(sector1[0].substring(0, 8)), "ASCII");
		}

		public static String readBeneficiaryCardIdAsString(String[] sector1) throws UnsupportedEncodingException
		{
			return new String(Common.hexStringToByteArray(sector1[1]), "ASCII");
		}

		public static String readFullName(String[] sector0, int version) throws UnsupportedEncodingException
		{
			switch (version)
			{
			case 1:
			{
				String name = new String(Common.hexStringToByteArray(sector0[1]), "UTF-8").trim();
				String surname = new String(Common.hexStringToByteArray(sector0[2]), "UTF-8").trim();
				return (name + " " + surname).trim();
			}
			case 2:
			{
				byte[] fullNameBytes = Common.hexStringToByteArray(sector0[1] + sector0[2]);
				String fullName = new String(fullNameBytes, "UTF-8").trim();
				if (fullName == null)
					fullName = "";
				return fullName.trim();
			}
			default:
			{
				String name = new String(Common.hexStringToByteArray(sector0[1]), "ASCII").trim();
				String surname = new String(Common.hexStringToByteArray(sector0[2]), "ASCII").trim();
				return (name + " " + surname).trim();
			}
			}
		}
	}

	public static class BluetoothPrinter
	{
		private BluetoothAdapter adapter;
		private static final UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
		private Context context;
		private Handler handler;
		private static final int DEVICE_CLASS_PRINTER = 0x680;

		public BluetoothPrinter(Context context) throws IOException
		{
			this.context = context;
			adapter = BluetoothAdapter.getDefaultAdapter();
			if (adapter == null)
				throw new IOException("No bluetooth devices present! Cannot initialize bluetooth services");

			handler = new Handler(Looper.getMainLooper());
		}

		//1111100000000
		//0011010000000

		//0011000000000
		//VTODO see: and implement device categories -detect printers- accordingly 
		//http://www.question-defense.com/tools/class-of-device-bluetooth-cod-list-in-binary-and-hex
		public boolean isBtOpen()
		{
			return adapter.isEnabled();
		}

		public List<BluetoothDevice> findPairedPrinters()
		{
			List<BluetoothDevice> retval = new ArrayList<BluetoothDevice>();

			Set<BluetoothDevice> pairedDevices = adapter.getBondedDevices();
			// If there are paired devices
			// Loop through paired devices
			for (BluetoothDevice device : pairedDevices)
			{
				BluetoothClass bluetoothClass = device.getBluetoothClass();
				boolean isPrinter = (bluetoothClass.getDeviceClass() == DEVICE_CLASS_PRINTER)
						|| (bluetoothClass.getDeviceClass() == 7936); //7936 -> Device class for TSC printers sadly
				if (isPrinter && device.getBondState() == BluetoothDevice.BOND_BONDED)
					retval.add(device);
			}
			return retval;
		}

		public boolean isBluetoothReadyToPrint()
		{
			if (isBtOpen() == false)
			{
				new AlertDialog.Builder(context).setTitle(R.string.dialog_title_bluetooth_pairing_required)
						.setMessage(R.string.dialog_bluetooth_pairing_required)
						.setIcon(android.R.drawable.ic_dialog_info)
						.setPositiveButton(R.string.msg_ok, new DialogInterface.OnClickListener()
						{
							public void onClick(DialogInterface dialog, int which)
							{
								Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
								context.startActivity(intent);
							}
						}).setNegativeButton(R.string.msg_cancel, new DialogInterface.OnClickListener()
						{
							@Override
							public void onClick(DialogInterface dialog, int which)
							{
								//Do nothing
							}
						}).show();

				return false;
			}
			else
				return true;
		}

		public static void printTestPage(Context ctx, String logo, String printFont, String printFontSize)
		{
			try
			{
				final int MAX_CHARS = 30;
				BluetoothPrinter printer = new BluetoothPrinter(ctx);
				if (printer.isBluetoothReadyToPrint())
				{
					StringBuilder sb = new StringBuilder();
					sb.append("\n");
					sb.append("******************************\n");
					sb.append(center(ctx.getResources().getString(R.string.printing_succesful), MAX_CHARS));
					sb.append("\n");
					sb.append("******************************\n");
					sb.append("\n");
					sb.append("\n");
					sb.append("\n");
					sb.append("\n");
					sb.append("\n");
					sb.append("\n");
					sb.append("--------------------------------\n");
					sb.append("\n");
					sb.append("\n");

					printer.print(sb.toString(), 1, 1000, logo, printFont, printFontSize, true, null);
				}
			}
			catch (IOException e)
			{
				displayErrorMessage(ctx, R.string.dialog_title_generic_error, e.getMessage());
			}
		}

		public static byte[] SELECT_BIT_IMAGE_MODE = {0x1B, 0x2A, 33, -128, 0};

		public void print(final String message, int numberOfCopies, final long delayInMillis, final String logoFile,
				final String printFont, final String printFontSize) throws IOException
		{
			print(message, numberOfCopies, delayInMillis, logoFile, printFont, printFontSize, false, null);
		}

		public void print(final String message, int numberOfCopies, final long delayInMillis, final String logoFile,
				final String printFont, final String printFontSize, final boolean uploadFont,
				final AlertDialog externalDialog) throws IOException
		{
			final List<BluetoothDevice> devices = findPairedPrinters();
			if (devices.size() == 0)
			{
				//logger.warn("No paired printers not found! Please pair a bluetooth printer before printing!");
				return;
			}

			final int[] internalNumCopies = new int[] {numberOfCopies};
			class PrintTask extends AsyncTask<String, Void, Void>
			{
				AlertDialog d;

				@Override
				protected void onPreExecute()
				{
					d = new AlertDialog.Builder(context) //
							.setTitle(R.string.dialog_title_printing) //
							.setMessage(R.string.dialog_printing) //
							.setIcon(R.drawable.print) //
							.create();

					d.show();
					d.setCancelable(false);
					d.setCanceledOnTouchOutside(false);
					d.show();

					super.onPreExecute();
				}

				@Override
				protected void onCancelled()
				{
					d.cancel();
					super.onCancelled();
				}

				@Override
				protected void onPostExecute(Void result)
				{
					d.dismiss();
					super.onPostExecute(result);
					if (delayInMillis <= 0) //If no delay is specified then open a popup and ask the user for next copy
					{
						internalNumCopies[0]--;
						if (internalNumCopies[0] > 0)
						{
							AlertDialog d2 = new AlertDialog.Builder(context) //
									.setTitle(R.string.dialog_title_printing) //
									.setMessage(R.string.dialog_click_ok_for_next_copy) //
									.setIcon(R.drawable.print) //
									.setPositiveButton(R.string.msg_ok, new DialogInterface.OnClickListener()
									{
										@Override
										public void onClick(DialogInterface dialog, int which)
										{
											new PrintTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, message, "1");
										}
									}).create();

							d2.setCancelable(false);
							d2.setCanceledOnTouchOutside(false);
							d2.show();
						}
						else if (externalDialog != null)
						{
							externalDialog.show();
						}

					}
				}

				@Override
				protected Void doInBackground(String... params)
				{
					int trial = 0;
					while (true)
					{
						// Cancel discovery because it will slow down the connection
						adapter.cancelDiscovery();
						List<String> exceptions = new ArrayList<String>();
						boolean success = false;
						for (BluetoothDevice device : devices)
						{
							BluetoothSocket socket = null;
							try
							{
								//socket = device.createRfcommSocketToServiceRecord(SPP_UUID);
								socket = device.createInsecureRfcommSocketToServiceRecord(SPP_UUID);
								socket.connect();

								String text = params[0];
								int numberOfCopies = Integer.parseInt(params[1]);
								if (device.getName().toLowerCase(Locale.US).contains("argox"))
								{
									for (int i = 0; i < numberOfCopies; i++)
										printPPLZ(socket, text);
								}
								else if (device.getName().toLowerCase(Locale.US).contains("bt-spp"))
								{
									printTSPL2(socket, text, numberOfCopies, delayInMillis, logoFile, printFont,
											printFontSize, uploadFont);
								}
								else
								{
									for (int i = 0; i < numberOfCopies; i++)
										printRaw(socket, text);

								}
								success = true; //If we came here that means we are successful
								break;
							}
							catch (final IOException e)
							{
								String message = e.getMessage();
								if (!exceptions.contains(message))
									exceptions.add(message);
							}
							finally
							{
								if (socket != null && socket.isConnected())
								{
									try
									{
										try
										{
											Thread.sleep(1000L);
										}
										catch (InterruptedException e)
										{
										}
										socket.close();
										try
										{
											Thread.sleep(2000L);
										}
										catch (InterruptedException e)
										{
										}
									}
									catch (IOException e)
									{
										String message = e.getMessage();
										if (!exceptions.contains(message))
											exceptions.add(message);
									}
								}
							}
						}

						if (!success && exceptions.size() > 0)
						{
							trial++;
							if (trial < 2)
							{
								postToast(context.getResources().getString(R.string.printing_taking_longer));
								continue;
							}
							postErrorMessage(exceptions);
						}
						break;
					}

					return null;
				}

				private byte[] mergeByteArrays(byte[] first, byte[] second)
				{
					byte[] merged = new byte[first.length + second.length];
					System.arraycopy(first, 0, merged, 0, first.length);
					System.arraycopy(second, 0, merged, first.length, second.length);
					return merged;
				}

				private void printTSPL2(BluetoothSocket socket, String text, int numCopies, long delayInMillis,
						String logoFile, String printFont, String printFontSize, boolean uploadFont) throws IOException
				{
					int INCREMENT = 32;
					final int leftMargin = 0;
					final double PAGE_HEIGHT = 11.69; //11.69 inches A4 Height
					double DPI = 203;
					int SPEED = 2; //inch per second

					int LOGOHEIGHT = 0;
					byte[] logoData = null;
					if (!isEmpty(logoFile))
					{
						try
						{
							InputStream is = context.getAssets().open("logo/" + logoFile);
							byte[] logobytes = new byte[is.available()];
							is.read(logobytes);
							is.close();
							LOGOHEIGHT = 110;

							byte[] logoHeader = String.format(Locale.US, "DOWNLOAD \"LOGO.BMP\",%d,", logobytes.length)
									.getBytes("ASCII");
							logoData = mergeByteArrays(logoHeader, logobytes);
						}
						catch (IOException e)
						{
							/*logger.warn("Logo file " + logoFile + " has caused an error, disabling logo printing: "
									+ e.getMessage());*/
						}
					}

					byte[] fontData = null;
					if (!isEmpty(printFont) && uploadFont)
					{
						try
						{
							InputStream is = context.getAssets().open("font/" + printFont);
							byte[] fontBytes = new byte[is.available()];
							is.read(fontBytes);
							is.close();
							byte[] fontHeader = String.format(Locale.US, "DOWNLOAD F,\"FONT.TTF\",%d,",
									fontBytes.length).getBytes("ASCII");
							fontData = mergeByteArrays(fontHeader, fontBytes);
						}
						catch (IOException e)
						{
							/*logger.warn("Print font " + printFont + " has caused an error, failed font upload: "
									+ e.getMessage());*/
							throw e;
						}
					}

					if (isEmpty(printFontSize))
						printFontSize = "10.5";

					List<String> allLines = Arrays.asList(text.split("\\r?\\n"));
					List<List<String>> pages = new ArrayList<List<String>>();
					int linesFirstPage = (int)((PAGE_HEIGHT * DPI - LOGOHEIGHT) / INCREMENT) - 3; //Additional 3 lines is required to print page number and tear off space
					int linesPerPage = (int)(PAGE_HEIGHT * DPI / INCREMENT) - 3; //Additional 3 lines is required to print page number and tear off space
					List<String> currentPage = null;
					for (int i = 0; i < allLines.size(); i++)
					{
						boolean split = false;
						if (pages.size() <= 1 && i % linesFirstPage == 0) //if first page
							split = true;
						else if ((i - linesFirstPage) % linesPerPage == 0) //other pages
							split = true;

						if (split)
						{
							currentPage = new ArrayList<String>();
							pages.add(currentPage);
						}

						currentPage.add(allLines.get(i));
					}

					StringBuilder sb = new StringBuilder();
					for (int p = 0; p < pages.size(); p++)
					{
						List<String> lines = pages.get(p);
						if (pages.size() > 1)
						{
							lines.add(0, ""); //Insert to front
							lines.add("[" + (p + 1) + " / " + (pages.size() + "]"));
							lines.add("--------------------------------");
						}

						double height = (double)(INCREMENT * lines.size()) / DPI; //height in inches
						if (p == 0)
							height += (double)LOGOHEIGHT / (double)DPI;
						String heightStr = BigDecimal.valueOf(height).setScale(2, BigDecimal.ROUND_HALF_UP)
								.toPlainString();

						sb.append("CODEPAGE 8859-15\n");
						sb.append("SIZE 2," + heightStr + "\n");
						sb.append("GAP 0,0\n");
						sb.append("DIRECTION 0\n");
						sb.append("SPEED " + SPEED + "\n");
						sb.append("DENSITY 15\n");
						sb.append("CLS\n");

						if (!isEmpty(printFont))
						{
							sb.append("J=LOF(\"FONT.TTF\")\n");
							sb.append("IF J > 0 THEN\n");
							sb.append("FONT$=\"FONT.TTF\"\n");
							sb.append("FONTSIZE=" + printFontSize + "\n");
							sb.append("ELSE\n");
							sb.append("FONT$=\"D.FNT\"\n");
							sb.append("FONTSIZE=1\n");
							sb.append("ENDIF\n");
						}
						else
						{
							sb.append("FONT$=\"D.FNT\"\n");
							sb.append("FONTSIZE=1\n");
						}

						for (int i = 0; i < lines.size(); i++)
						{
							sb.append("TEXT ");
							sb.append(leftMargin);
							sb.append(",");
							if (p == 0)
							{
								if (i < 2) //On first page, shift every line after first 2 lines by LOGOHEIGHT
									sb.append(INCREMENT * i);
								else
									sb.append((INCREMENT * i) + LOGOHEIGHT);
							}
							else
							{
								sb.append(INCREMENT * i);
							}

							//sb.append(",\"D.FNT\",0,1,1,\"");

							sb.append(",FONT$,0,FONTSIZE,FONTSIZE,\"");

							sb.append(lines.get(i));
							sb.append("\"\n");

							if (LOGOHEIGHT > 0 && p == 0 && i == 0)
								sb.append("PUTBMP 150,60,\"LOGO.BMP\"\n");
						}
						sb.append("PRINT 1\n");
						sb.append("EOJ\n");
						sb.append("DELAY 1000\n");
					}

					StringBuilder sbCopies = new StringBuilder();
					sbCopies.append("DOWNLOAD \"PRINT.BAS\"\n");
					for (int copy = 0; copy < numCopies; copy++)
					{
						sbCopies.append(sb);
						sb.append("EOJ\n");
						if (delayInMillis > 0)
							sbCopies.append("DELAY " + delayInMillis + "\n");
					}
					sbCopies.append("EOP\n");
					sbCopies.append("RUN \"PRINT.BAS\"\n");
					sbCopies.append("EOP\n");

					byte[] data = sbCopies.toString().getBytes(Charset.forName("UTF-8"));
					if (logoData != null)
						data = mergeByteArrays(logoData, data);
					if (fontData != null)
						data = mergeByteArrays(fontData, data);

					socket.getOutputStream().write(data);
					socket.getOutputStream().flush();
				}

				private void printRaw(BluetoothSocket socket, String text) throws IOException
				{
					byte[] data = text.getBytes(Charset.forName("ASCII"));
					socket.getOutputStream().write(data);
					socket.getOutputStream().flush();
				}

				private void printPPLZ(BluetoothSocket socket, String text) throws IOException
				{
					int INCREMENT = 22;
					final int leftMargin = 10;
					StringBuilder sb = new StringBuilder();
					sb.append("^XA^POI^MCY^PMN^PW406^MNN^LL30^JZY^LH0,0^LRI^XZ\r\n");
					sb.append("^XA");
					String[] lines = text.split("\\r?\\n");
					for (int i = 0; i < lines.length; i++)
					{
						//Prints something like ^FO10,44^AC^FDTextToPrint^FS
						sb.append("^FO");
						sb.append(leftMargin);
						sb.append(",");
						sb.append(INCREMENT * i);
						sb.append("^AC^FD");
						sb.append(lines[i] + "\r\n");
						sb.append("^FS\r\n");
					}
					sb.append("^XZ");
					byte[] data = sb.toString().getBytes(Charset.forName("ASCII"));
					socket.getOutputStream().write(data);
					socket.getOutputStream().flush();
				}

				private void postToast(final String message)
				{
					handler.post(new Runnable()
					{
						@Override
						public void run()
						{
							Toast.makeText(context, context.getString(R.string.printing_taking_longer),
									Toast.LENGTH_SHORT).show();
						}
					});

				}

				private void postErrorMessage(final List<String> exceptions)
				{
					handler.post(new Runnable()
					{
						@Override
						public void run()
						{
							//						StringBuilder sb = new StringBuilder();
							//						for (String e : exceptions)
							//						{
							//							sb.append(e);
							//							sb.append("\n");
							//						}

							displayErrorMessage(context, R.string.dialog_title_generic_error,
									context.getString(R.string.dialog_printing_failed));
						}
					});
				}
			}

			if (delayInMillis <= 0) //This means we'll pop up for next print so send only 1 copy request, see internalNumCopies to understand the rest
				new PrintTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, message, "1");
			else
				new PrintTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, message, numberOfCopies + "");
		}
	}

	@Override
	protected void onNewIntent(Intent intent)
	{
		nfcReceiver.readTag(intent);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState)
	{
		super.onSaveInstanceState(outState);
		outState.putString(INTENT_DATA_BENEFICIARY_FULL_NAME, beneficiaryFullName.getText().toString());
		outState.putString(INTENT_DATA_SLIP_COMPANY_NAME, printParams.slipCompanyName);
		outState.putString(INTENT_DATA_SLIP_HEADER_VOUCHER, printParams.slipHeaderVoucher);
		outState.putString(INTENT_DATA_SLIP_HEADER_LOGO, printParams.logo);
		outState.putString(INTENT_DATA_SLIP_PRINT_FONT, printParams.printFont);
		outState.putString(INTENT_DATA_SLIP_PRINT_FONT_SIZE, printParams.printFontSize);
		outState.putString(INTENT_DATA_HOTLINE_NUMBER, printParams.hotlineNumber);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState)
	{
		super.onRestoreInstanceState(savedInstanceState);
		beneficiaryFullName.setText(savedInstanceState.getString(INTENT_DATA_BENEFICIARY_FULL_NAME));
		printParams.slipCompanyName = savedInstanceState.getString(INTENT_DATA_SLIP_COMPANY_NAME);
		printParams.slipHeaderVoucher = savedInstanceState.getString(INTENT_DATA_SLIP_HEADER_VOUCHER);
		printParams.logo = savedInstanceState.getString(INTENT_DATA_SLIP_HEADER_LOGO);
		printParams.printFont = savedInstanceState.getString(INTENT_DATA_SLIP_PRINT_FONT);
		printParams.printFontSize = savedInstanceState.getString(INTENT_DATA_SLIP_PRINT_FONT_SIZE);
		printParams.hotlineNumber = savedInstanceState.getString(INTENT_DATA_HOTLINE_NUMBER);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		nfcReceiver = new NFCReceiver(this)
		{
			@Override
			public void onCardRead(String chipID, MCReader reader) throws IOException, IllegalArgumentException
			{
				ActivateBeneficiaryCardActivity.this.onCardRead(chipID, reader);
			}

		};
		setContentView(R.layout.activity_activate_beneficiary_card);

		String beneficiaryFullName = (String)getIntent().getSerializableExtra(INTENT_DATA_BENEFICIARY_FULL_NAME);
		try
		{
			readCard = Boolean.parseBoolean((String)getIntent().getSerializableExtra(INTENT_DATA_READ_CARD));
		}
		catch (RuntimeException re)
		{
			readCard = false;
		}
		if (!readCard)
		{
			if (isEmpty(beneficiaryFullName))
				throw new IllegalArgumentException("beneficiary_full_name intent data parameter is required!");
		}

		printParams = new PrintParams();
		printParams.slipCompanyName = (String)getIntent().getSerializableExtra(INTENT_DATA_SLIP_COMPANY_NAME);
		printParams.slipHeaderVoucher = (String)getIntent().getSerializableExtra(INTENT_DATA_SLIP_HEADER_VOUCHER);
		printParams.logo = (String)getIntent().getSerializableExtra(INTENT_DATA_SLIP_HEADER_LOGO);
		printParams.printFont = (String)getIntent().getSerializableExtra(INTENT_DATA_SLIP_PRINT_FONT);
		printParams.printFontSize = (String)getIntent().getSerializableExtra(INTENT_DATA_SLIP_PRINT_FONT_SIZE);
		printParams.hotlineNumber = (String)getIntent().getSerializableExtra(INTENT_DATA_HOTLINE_NUMBER);

		this.beneficiaryFullName = (TextView)findViewById(R.id.beneficiaryFullName);
		this.beneficiaryFullName.setText(beneficiaryFullName);

		/*// Set up the action bar.
		final ActionBar actionBar = getActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);*/

		rng = new Random();
	}

	protected void onCardRead(final String chipID, final MCReader reader) throws IOException
	{
		UUID beneficiaryCardUUID = null;
		String chipIdExt;

		String[] sector0 = reader.readSector0(Common.hexStringToByteArray("FFFFFFFFFFFF"));
		String[] sector1 = reader.readSector1(Common.hexStringToByteArray("FFFFFFFFFFFF"));
		if (sector0 == null || sector1 == null)
			throw new IllegalArgumentException("Invalid Card!");

		int cardVersion = NFCReceiver.readCardVersion(sector1);
		chipIdExt = NFCReceiver.readChipIdExtension(sector1);

		if (!isBlank(NFCReceiver.readBeneficiaryCardIdAsString(sector1)))
		{
			byte[] mostSignificantBits = Common.hexStringToByteArray(sector1[1].substring(0, 16));
			byte[] leastSignificantBits = Common.hexStringToByteArray(sector1[1].substring(16, 32));
			beneficiaryCardUUID = new UUID(fromByteArray(mostSignificantBits), fromByteArray(leastSignificantBits));
		}

		if (!isBlank(chipIdExt) && !readCard)
		{
			String fullName = NFCReceiver.readFullName(sector0, cardVersion);
			throw new IOException(getString(R.string.card_is_already_activated_for, fullName));
		}

		if (readCard)
		{
			if (isBlank(chipIdExt))
				throw new IOException("Card is invalid or not activated yet!");
			else
			{
				StringBuilder sb = new StringBuilder();
				if (beneficiaryCardUUID != null)
					sb.append(beneficiaryCardUUID);
				sb.append("|");
				sb.append(chipID);
				sb.append("|");
				sb.append(chipIdExt);
				sb.append("|");
				sb.append("");
				String encodedRetval = Base64.encodeToString(sb.toString().getBytes("ASCII"), Base64.NO_WRAP);
				Intent returnIntent = new Intent();
				returnIntent.putExtra(INTENT_DATA_VALUE, encodedRetval);
				setResult(RESULT_OK, returnIntent);
				ActivateBeneficiaryCardActivity.this.finish();
				return;
			}
		}

		//VahapT: Please note that we've removed duplicate card activation prevention algorithms.
		//ODK version of this class does not write anything to the database thus prevention is not possible.

		//Continue with full activation
		byte[] keyB0Bytes = NFCReceiver.calculateKeyB(chipID, 0);
		byte[] keyB1Bytes = NFCReceiver.calculateKeyB(chipID, 1);
		byte[] keyB15Bytes = NFCReceiver.calculateKeyB(chipID, 15);

		byte[] fullNameBytes = beneficiaryFullName.getText().toString().getBytes("UTF-8");
		Pair<byte[], byte[]> nameSectors = make2x16Byte(fullNameBytes);

		if (beneficiaryCardUUID == null)
		{
			beneficiaryCardUUID = UUID.randomUUID();
		}
		ByteBuffer buf = ByteBuffer.allocate(16);
		buf.putLong(beneficiaryCardUUID.getMostSignificantBits());
		buf.putLong(beneficiaryCardUUID.getLeastSignificantBits());
		byte[] beneficiaryCardIdBytes = buf.array();
		byte[] versionBytes = make16Byte(new byte[] {2});
		final int pinCode = rng.nextInt(9000) + 1000;

		//Now generate chipIDExtension
		chipIdExt = random(4, 0, 0, true, false, null, rng).toUpperCase(Locale.US);
		byte[] chipIdExtBytes = make16Byte(chipIdExt.getBytes("ASCII"));

		String pinCodeStr = String.valueOf(pinCode);
		byte[] pinCodeBytes = make16Byte(pinCodeStr.getBytes("ASCII"));

		reader.writeBlockExcept(0, 1, nameSectors.first, keyB0Bytes, true);
		reader.writeBlockExcept(0, 2, nameSectors.second, keyB0Bytes, true);

		reader.writeBlockExcept(1, 1, beneficiaryCardIdBytes, keyB1Bytes, true);
		reader.writeBlockExcept(1, 2, versionBytes, keyB1Bytes, true);
		reader.writeBlockExcept(15, 2, pinCodeBytes, keyB15Bytes, true);

		reader.writeBlockExcept(1, 0, chipIdExtBytes, keyB1Bytes, true); //Write this last

		StringBuilder sb = new StringBuilder();
		sb.append(beneficiaryCardUUID.toString());
		sb.append("|");
		sb.append(chipID);
		sb.append("|");
		sb.append(chipIdExt);
		sb.append("|");
		sb.append(pinCode);
		String encodedRetval = Base64.encodeToString(sb.toString().getBytes("ASCII"), Base64.NO_WRAP);
		Intent returnIntent = new Intent();
		returnIntent.putExtra(INTENT_DATA_VALUE, encodedRetval);
		setResult(RESULT_OK, returnIntent);

		final String chipIdExtF = chipIdExt;
		final AlertDialog[] dialogHolder = new AlertDialog[1];
		dialogHolder[0] = new AlertDialog.Builder(this).setTitle(getString(R.string.card))
				.setMessage(getString(R.string.card_is_activated_for, beneficiaryFullName.getText()))
				.setIcon(android.R.drawable.ic_dialog_alert)
				.setNeutralButton(R.string.print_again, new DialogInterface.OnClickListener()
				{
					@Override
					public void onClick(DialogInterface dialog, int which)
					{
						printSlip(ActivateBeneficiaryCardActivity.this.printParams, chipID, chipIdExtF, pinCode,
								dialogHolder[0]);
					}

				}).setPositiveButton(R.string.msg_ok, new DialogInterface.OnClickListener()
				{
					public void onClick(DialogInterface dialog, int which)
					{
						ActivateBeneficiaryCardActivity.this.finish();
					}
				}).create();
		printSlip(this.printParams, chipID, chipIdExt, pinCode, dialogHolder[0]);
	}

	private void appendToCenter(StringBuilder stringBuilder, String string)
	{
		final int MAX_CHARS = 32;

		int spaceCount = MAX_CHARS - string.length();
		for (int i = 0; i < spaceCount / 2; i++)
			stringBuilder.append(" ");
		stringBuilder.append(string);
		for (int i = 0; i < spaceCount / 2; i++)
			stringBuilder.append(" ");

		stringBuilder.append("\n");
	}

	private void printSlip(PrintParams p, String chipId, String chipIdExt, int pinCode, final AlertDialog dialog)
	{
		try
		{
			BluetoothPrinter printer = new BluetoothPrinter(this);
			if (printer.isBluetoothReadyToPrint())
			{
				final int MAX_CHARS = 32;
				int spaceCount;

				StringBuilder sb = new StringBuilder();
				sb.append("\n");

				if (!isEmpty(p.slipCompanyName))
					appendToCenter(sb, p.slipCompanyName);
				if (!isEmpty(p.slipHeaderVoucher))
					appendToCenter(sb, p.slipHeaderVoucher);
				appendToCenter(sb, getString(R.string.beneficiary_card));
				sb.append("\n");

				sb.append(getString(R.string.beneficiary) + beneficiaryFullName.getText());
				sb.append("\n");
				sb.append(getString(R.string.card_id) + chipId + "-" + chipIdExt);
				sb.append("\n");

				sb.append(getString(R.string.pincode) + pinCode);
				sb.append("\n");
				sb.append("\n");

				if (!isEmpty(p.hotlineNumber))
				{
					String s = getString(R.string.please_call, "\n" + p.hotlineNumber + "\n");
					String[] strings = s.split("\\n");
					for (int i = 0; i < strings.length; i++)
					{
						appendToCenter(sb, strings[i]);
					}
					sb.append("\n");
				}

				StringBuilder hashInput = new StringBuilder();
				hashInput.append(chipId);
				hashInput.append(chipIdExt);
				hashInput.append(pinCode);
				String hashLabel = getString(R.string.printer_hashlabel);
				String hash = calculateHash(hashInput.toString());
				spaceCount = MAX_CHARS - (hashLabel.length() + hash.length());
				sb.append(hashLabel);
				for (int i = 0; i < spaceCount; i++)
					sb.append(" ");
				sb.append(hash);
				sb.append("\n");
				sb.append("\n");
				sb.append("\n");
				sb.append("\n");
				sb.append("--------------------------------\n");
				sb.append("\n");
				sb.append("\n");

				printer.print(sb.toString(), 1, 0, p.logo, p.printFont, p.printFontSize, false, dialog);
				//Utils.printToPopup(ActivateBeneficiaryCardActivity.this, sb);

			}
		}
		catch (IOException e)
		{
			displayErrorMessage(this, R.string.dialog_title_generic_error, e.getMessage());
		}
	}

	private String calculateHash(String input)
	{
		try
		{
			input = "jrwored!_52" + input + "mvfe-?rose";
			byte[] bytes = input.getBytes(Charset.forName("ASCII"));
			MessageDigest md = MessageDigest.getInstance("SHA");
			md.update(bytes, 0, bytes.length);
			byte[] hash = md.digest();
			byte[] key = new byte[4];
			System.arraycopy(hash, 0, key, 0, key.length);
			return bytesToHex(key);
		}
		catch (NoSuchAlgorithmException e)
		{
			return "FAILED!";
		}
	}

	private byte[] make16Byte(byte[] input)
	{
		byte[] retval = new byte[16];
		for (int i = 0; i < retval.length && i < input.length; i++)
			retval[i] = input[i];
		return retval;
	}

	private Pair<byte[], byte[]> make2x16Byte(byte[] input)
	{
		byte[] a = new byte[16];
		byte[] b = new byte[16];
		int i = 0;
		for (; i < a.length && i < input.length; i++)
			a[i] = input[i];
		for (; i < b.length + a.length && i < input.length; i++)
			b[i - a.length] = input[i];
		return new Pair<byte[], byte[]>(a, b);
	}

	public static void displayErrorMessage(Context context, int titleID, String message)
	{
		new AlertDialog.Builder(context).setTitle(titleID).setMessage(message)
				.setIcon(android.R.drawable.ic_dialog_alert)
				.setPositiveButton(R.string.msg_ok, new DialogInterface.OnClickListener()
				{
					public void onClick(DialogInterface dialog, int which)
					{
						//Nothing to do
					}
				}).show();
	}

	/**
	 * Convert byte array to hex string
	 * @param bytes
	 * @return
	 */
	public static String bytesToHex(byte[] bytes)
	{
		StringBuilder sbuf = new StringBuilder();
		for (int idx = 0; idx < bytes.length; idx++)
		{
			int intVal = bytes[idx] & 0xff;
			if (intVal < 0x10)
				sbuf.append("0");
			sbuf.append(Integer.toHexString(intVal).toUpperCase(Locale.US));
		}
		return sbuf.toString();
	}

	public static byte[] hexStringToByteArray(String s)
	{
		int len = s.length();
		byte[] data = new byte[len / 2];
		for (int i = 0; i < len; i += 2)
		{
			data[i / 2] = (byte)((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i + 1), 16));
		}
		return data;
	}

	//StringUtils
	private static final int PAD_LIMIT = 8192;
	public static final String SPACE = " ";

	public static boolean isEmpty(final CharSequence cs)
	{
		return cs == null || cs.length() == 0;
	}

	public static boolean isBlank(final CharSequence cs)
	{
		int strLen;
		if (cs == null || (strLen = cs.length()) == 0)
		{
			return true;
		}
		for (int i = 0; i < strLen; i++)
		{
			if (Character.isWhitespace(cs.charAt(i)) == false)
			{
				return false;
			}
		}
		return true;
	}

	public static String center(final String str, final int size)
	{
		return center(str, size, ' ');
	}

	public static String center(String str, final int size, final char padChar)
	{
		if (str == null || size <= 0)
		{
			return str;
		}
		final int strLen = str.length();
		final int pads = size - strLen;
		if (pads <= 0)
		{
			return str;
		}
		str = leftPad(str, strLen + pads / 2, padChar);
		str = rightPad(str, size, padChar);
		return str;
	}

	public static String leftPad(final String str, final int size, final char padChar)
	{
		if (str == null)
		{
			return null;
		}
		final int pads = size - str.length();
		if (pads <= 0)
		{
			return str; // returns original String when possible
		}
		if (pads > PAD_LIMIT)
		{
			return leftPad(str, size, String.valueOf(padChar));
		}
		return repeat(padChar, pads).concat(str);
	}

	public static String leftPad(final String str, final int size, String padStr)
	{
		if (str == null)
		{
			return null;
		}
		if (isEmpty(padStr))
		{
			padStr = SPACE;
		}
		final int padLen = padStr.length();
		final int strLen = str.length();
		final int pads = size - strLen;
		if (pads <= 0)
		{
			return str; // returns original String when possible
		}
		if (padLen == 1 && pads <= PAD_LIMIT)
		{
			return leftPad(str, size, padStr.charAt(0));
		}

		if (pads == padLen)
		{
			return padStr.concat(str);
		}
		else if (pads < padLen)
		{
			return padStr.substring(0, pads).concat(str);
		}
		else
		{
			final char[] padding = new char[pads];
			final char[] padChars = padStr.toCharArray();
			for (int i = 0; i < pads; i++)
			{
				padding[i] = padChars[i % padLen];
			}
			return new String(padding).concat(str);
		}
	}

	public static String repeat(final char ch, final int repeat)
	{
		final char[] buf = new char[repeat];
		for (int i = repeat - 1; i >= 0; i--)
		{
			buf[i] = ch;
		}
		return new String(buf);
	}

	public static String rightPad(final String str, final int size, final char padChar)
	{
		if (str == null)
		{
			return null;
		}
		final int pads = size - str.length();
		if (pads <= 0)
		{
			return str; // returns original String when possible
		}
		if (pads > PAD_LIMIT)
		{
			return rightPad(str, size, String.valueOf(padChar));
		}
		return str.concat(repeat(padChar, pads));
	}

	public static long fromByteArray(byte[] bytes)
	{
		return fromBytes(bytes[0], bytes[1], bytes[2], bytes[3], bytes[4], bytes[5], bytes[6], bytes[7]);
	}

	public static long fromBytes(byte b1, byte b2, byte b3, byte b4, byte b5, byte b6, byte b7, byte b8)
	{
		return (b1 & 0xFFL) << 56 | (b2 & 0xFFL) << 48 | (b3 & 0xFFL) << 40 | (b4 & 0xFFL) << 32 | (b5 & 0xFFL) << 24
				| (b6 & 0xFFL) << 16 | (b7 & 0xFFL) << 8 | (b8 & 0xFFL);
	}

	public static String rightPad(final String str, final int size, String padStr)
	{
		if (str == null)
		{
			return null;
		}
		if (isEmpty(padStr))
		{
			padStr = SPACE;
		}
		final int padLen = padStr.length();
		final int strLen = str.length();
		final int pads = size - strLen;
		if (pads <= 0)
		{
			return str; // returns original String when possible
		}
		if (padLen == 1 && pads <= PAD_LIMIT)
		{
			return rightPad(str, size, padStr.charAt(0));
		}

		if (pads == padLen)
		{
			return str.concat(padStr);
		}
		else if (pads < padLen)
		{
			return str.concat(padStr.substring(0, pads));
		}
		else
		{
			final char[] padding = new char[pads];
			final char[] padChars = padStr.toCharArray();
			for (int i = 0; i < pads; i++)
			{
				padding[i] = padChars[i % padLen];
			}
			return str.concat(new String(padding));
		}
	}

	public static String random(int count, int start, int end, final boolean letters, final boolean numbers,
			final char[] chars, final Random random)
	{
		if (count == 0)
		{
			return "";
		}
		else if (count < 0)
		{
			throw new IllegalArgumentException("Requested random string length " + count + " is less than 0.");
		}
		if (chars != null && chars.length == 0)
		{
			throw new IllegalArgumentException("The chars array must not be empty");
		}

		if (start == 0 && end == 0)
		{
			if (chars != null)
			{
				end = chars.length;
			}
			else
			{
				if (!letters && !numbers)
				{
					end = Integer.MAX_VALUE;
				}
				else
				{
					end = 'z' + 1;
					start = ' ';
				}
			}
		}
		else
		{
			if (end <= start)
			{
				throw new IllegalArgumentException("Parameter end (" + end + ") must be greater than start (" + start
						+ ")");
			}
		}

		final char[] buffer = new char[count];
		final int gap = end - start;

		while (count-- != 0)
		{
			char ch;
			if (chars == null)
			{
				ch = (char)(random.nextInt(gap) + start);
			}
			else
			{
				ch = chars[random.nextInt(gap) + start];
			}
			if (letters && Character.isLetter(ch) || numbers && Character.isDigit(ch) || !letters && !numbers)
			{
				if (ch >= 56320 && ch <= 57343)
				{
					if (count == 0)
					{
						count++;
					}
					else
					{
						// low surrogate, insert high surrogate after putting it in
						buffer[count] = ch;
						count--;
						buffer[count] = (char)(55296 + random.nextInt(128));
					}
				}
				else if (ch >= 55296 && ch <= 56191)
				{
					if (count == 0)
					{
						count++;
					}
					else
					{
						// high surrogate, insert low surrogate before putting it in
						buffer[count] = (char)(56320 + random.nextInt(128));
						count--;
						buffer[count] = ch;
					}
				}
				else if (ch >= 56192 && ch <= 56319)
				{
					// private high surrogate, no effing clue, so skip it
					count++;
				}
				else
				{
					buffer[count] = ch;
				}
			}
			else
			{
				count++;
			}
		}
		return new String(buffer);
	}
}
