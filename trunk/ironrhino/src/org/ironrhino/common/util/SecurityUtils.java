package org.ironrhino.common.util;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.Security;
import java.security.SignatureException;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;

import org.bouncycastle.bcpg.ArmoredInputStream;
import org.bouncycastle.bcpg.ArmoredOutputStream;
import org.bouncycastle.bcpg.BCPGOutputStream;
import org.bouncycastle.crypto.generators.ElGamalParametersGenerator;
import org.bouncycastle.crypto.params.ElGamalParameters;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ElGamalParameterSpec;
import org.bouncycastle.openpgp.PGPCompressedData;
import org.bouncycastle.openpgp.PGPCompressedDataGenerator;
import org.bouncycastle.openpgp.PGPEncryptedData;
import org.bouncycastle.openpgp.PGPEncryptedDataGenerator;
import org.bouncycastle.openpgp.PGPEncryptedDataList;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPKeyPair;
import org.bouncycastle.openpgp.PGPKeyRingGenerator;
import org.bouncycastle.openpgp.PGPLiteralData;
import org.bouncycastle.openpgp.PGPLiteralDataGenerator;
import org.bouncycastle.openpgp.PGPObjectFactory;
import org.bouncycastle.openpgp.PGPOnePassSignature;
import org.bouncycastle.openpgp.PGPOnePassSignatureList;
import org.bouncycastle.openpgp.PGPPrivateKey;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyEncryptedData;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.bouncycastle.openpgp.PGPSecretKeyRing;
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection;
import org.bouncycastle.openpgp.PGPSignature;
import org.bouncycastle.openpgp.PGPSignatureGenerator;
import org.bouncycastle.openpgp.PGPSignatureList;
import org.bouncycastle.openpgp.PGPSignatureSubpacketGenerator;
import org.bouncycastle.openpgp.PGPUtil;

public class SecurityUtils {

	static {
		Security.addProvider(new BouncyCastleProvider());
	}

	public static void generatorKeyPair(String keyId, String password,
			OutputStream pub, OutputStream pri)
			throws NoSuchAlgorithmException, NoSuchProviderException,
			InvalidAlgorithmParameterException, PGPException, IOException {
		int iKeySize = 1024, iStrength = 0;
		KeyPairGenerator dsaKpg = KeyPairGenerator.getInstance("DSA", "BC");
		dsaKpg.initialize(iKeySize);

		KeyPair dsaKp = dsaKpg.generateKeyPair();
		KeyPairGenerator elgKpg = KeyPairGenerator.getInstance("ELGAMAL", "BC");

		if (iStrength > 0) {

			ElGamalParametersGenerator paramGen = new ElGamalParametersGenerator();
			paramGen.init(iKeySize, iStrength, new SecureRandom());
			ElGamalParameters genParams = paramGen.generateParameters();
			ElGamalParameterSpec elParams = new ElGamalParameterSpec(genParams
					.getP(), genParams.getG());
			elgKpg.initialize(elParams);
		} else {
			BigInteger g = new BigInteger(
					"153d5d6172adb43045b68ae8e1de1070b6137005686d29d3d73a7749199681ee5b212c9b96bfdcfa5b20cd5e3fd2044895d609cf9b410b7a0f12ca1cb9a428cc",
					16);
			BigInteger p = new BigInteger(
					"9494fec095f3b85ee286542b3836fc81a5dd0a0349b4c239dd38744d488cf8e31db8bcb7d33b41abb9e5a33cca9144b1cef332c94bf0573bf047a3aca98cdf3b",
					16);
			ElGamalParameterSpec elParams = new ElGamalParameterSpec(p, g);
			elgKpg.initialize(elParams);
		}

		KeyPair elgKp = elgKpg.generateKeyPair();

		PGPKeyPair dsaKeyPair = new PGPKeyPair(PGPPublicKey.DSA, dsaKp,
				new Date(), "BC");
		PGPKeyPair elgKeyPair = new PGPKeyPair(PGPPublicKey.ELGAMAL_ENCRYPT,
				elgKp, new Date(), "BC");

		PGPKeyRingGenerator keyRingGen = new PGPKeyRingGenerator(
				PGPSignature.POSITIVE_CERTIFICATION, dsaKeyPair, keyId,
				PGPEncryptedData.AES_256, password.toCharArray(),
				true /* Use SHA1 */, null /* hashedPcks */,
				null /* unhashedPcks */, new SecureRandom(), "BC");
		keyRingGen.addSubKey(elgKeyPair);

		OutputStream ostream = new ArmoredOutputStream(pub);
		ostream.write(keyRingGen.generatePublicKeyRing().getEncoded());
		ostream.close();
		ostream = new ArmoredOutputStream(pri);
		ostream.write(keyRingGen.generateSecretKeyRing().getEncoded());
		ostream.close();
	}

	public static PGPPublicKey parsePGPPublicKey(InputStream is)
			throws IOException {
		PGPPublicKeyRing ring = new PGPPublicKeyRing(new ArmoredInputStream(is));
		Iterator<PGPPublicKey> iter = ring.getPublicKeys();
		while (iter.hasNext()) {
			PGPPublicKey k = iter.next();
			if (k.isEncryptionKey())
				return k;
		}
		return null;
	}

	public static PGPPublicKeyRingCollection parsePGPPublicKeyRingCollection(
			InputStream is) throws IOException, PGPException {
		PGPPublicKeyRingCollection pubring = new PGPPublicKeyRingCollection(
				Collections.EMPTY_LIST);
		PGPPublicKeyRing newKey = new PGPPublicKeyRing(new ArmoredInputStream(
				is));
		pubring = PGPPublicKeyRingCollection.addPublicKeyRing(pubring, newKey);
		return pubring;
	}

	public static PGPSecretKeyRingCollection parsePGPSecretKeyRingCollection(
			InputStream is) throws IOException, PGPException {
		PGPSecretKeyRingCollection secring = new PGPSecretKeyRingCollection(
				Collections.EMPTY_LIST);
		BufferedInputStream iStream = new BufferedInputStream(is);
		PGPSecretKeyRing ring;
		iStream.mark(1024 * 128);
		ring = new PGPSecretKeyRing(new ArmoredInputStream(iStream));
		secring = PGPSecretKeyRingCollection.addSecretKeyRing(secring, ring);
		return secring;
	}

	public static void encrypt(InputStream in, OutputStream out,
			InputStream keyIs) throws IOException, NoSuchProviderException,
			PGPException {
		encrypt(in, out, keyIs, false, true);
	}

	public static void writeStreamToLiteralData(OutputStream out,
			char fileType, InputStream in) throws IOException {
		PGPLiteralDataGenerator lData = new PGPLiteralDataGenerator();
		OutputStream pOut = lData.open(out, fileType, "file", new Date(),
				new byte[1024]);
		byte[] buf = new byte[4096];
		int len;

		while ((len = in.read(buf)) > 0) {
			pOut.write(buf, 0, len);
		}
		lData.close();
		in.close();
	}

	public static void encrypt(InputStream in, OutputStream out,
			InputStream keyIs, boolean armor, boolean withIntegrityCheck)
			throws IOException, NoSuchProviderException, PGPException {
		if (armor)
			out = new ArmoredOutputStream(out);
		PGPPublicKey encKey = parsePGPPublicKey(keyIs);
		ByteArrayOutputStream bOut = new ByteArrayOutputStream();
		PGPCompressedDataGenerator comData = new PGPCompressedDataGenerator(
				PGPCompressedData.ZIP);
		writeStreamToLiteralData(comData.open(bOut), PGPLiteralData.BINARY, in);
		// PGPUtil.writeFileToLiteralData(comData.open(bOut),
		// PGPLiteralData.BINARY, file);
		comData.close();
		PGPEncryptedDataGenerator cPk = new PGPEncryptedDataGenerator(
				PGPEncryptedData.CAST5, withIntegrityCheck, new SecureRandom(),
				"BC");
		cPk.addMethod(encKey);
		byte[] bytes = bOut.toByteArray();
		OutputStream cOut = cPk.open(out, bytes.length);
		cOut.write(bytes);
		cOut.close();
		out.close();
	}

	public static void decrypt(InputStream data, OutputStream out,
			InputStream keyIs, char[] passwd) throws Exception {
		PGPSecretKeyRingCollection secring = parsePGPSecretKeyRingCollection(keyIs);
		PGPPublicKeyEncryptedData pbe = null;
		InputStream in = PGPUtil.getDecoderStream(data);
		PGPObjectFactory pgpF = new PGPObjectFactory(in);
		PGPEncryptedDataList enc;
		Object o = pgpF.nextObject();
		if (o == null)
			throw new Exception("Cannot recognize input data format");
		//
		// the first object might be a PGP marker packet.
		//
		if (o instanceof PGPEncryptedDataList) {
			enc = (PGPEncryptedDataList) o;
		} else {
			enc = (PGPEncryptedDataList) pgpF.nextObject();
		}

		//
		// find the secret key
		//
		Iterator encObjects = enc.getEncryptedDataObjects();
		if (!encObjects.hasNext())
			throw new Exception("No encrypted data");
		pbe = (PGPPublicKeyEncryptedData) encObjects.next();

		PGPPrivateKey sKey = null;
		PGPSecretKey secretKey = secring.getSecretKey(pbe.getKeyID());
		sKey = secretKey.extractPrivateKey(passwd, "BC");
		// sKey = findSecretKey(it, passwd);

		InputStream clear = pbe.getDataStream(sKey, "BC");

		PGPObjectFactory plainFact = new PGPObjectFactory(clear);

		Object message = plainFact.nextObject();
		Object sigLiteralData = null;
		PGPObjectFactory pgpFact = null;

		if (message instanceof PGPCompressedData) {
			PGPCompressedData cData = (PGPCompressedData) message;
			pgpFact = new PGPObjectFactory(cData.getDataStream());
			message = pgpFact.nextObject();
			if (message instanceof PGPOnePassSignatureList) {
				sigLiteralData = pgpFact.nextObject();
			}
		}

		if (message instanceof PGPLiteralData) {
			// Message is just encrypted
			processLiteralData((PGPLiteralData) message, out, null);
		} else if (message instanceof PGPOnePassSignatureList) {
			// Message is signed and encrypted
			// ... decrypt without checking signature
			processLiteralData((PGPLiteralData) sigLiteralData, out, null);
		} else
			throw new PGPException(
					"message is not a simple encrypted file - type unknown.");

		if (pbe.isIntegrityProtected())
			if (!pbe.verify())
				throw new Exception("Message failed integrity check");
	}

	private static String processLiteralData(PGPLiteralData ld,
			OutputStream out, PGPOnePassSignature ops) throws IOException,
			SignatureException {
		String outFileName = ld.getFileName();
		InputStream unc = ld.getInputStream();
		int ch;
		if (ops == null) {
			while ((ch = unc.read()) >= 0)
				out.write(ch);
		} else {
			while ((ch = unc.read()) >= 0) {
				out.write(ch);
				ops.update((byte) ch);
			}
		}
		return outFileName;
	}

	public static void sign(InputStream in, OutputStream out,
			InputStream keyIs, char[] pass) throws IOException,
			NoSuchAlgorithmException, NoSuchProviderException, PGPException,
			SignatureException {
		// PGPSecretKey key = null;
		BufferedInputStream iStream = new BufferedInputStream(keyIs);
		PGPSecretKeyRing ring;
		iStream.mark(1024 * 128);
		ring = new PGPSecretKeyRing(new ArmoredInputStream(iStream));
		PGPSecretKey key = ring.getSecretKey();
		PGPPrivateKey priK = key.extractPrivateKey(pass, "BC");

		PGPSignatureGenerator sGen = new PGPSignatureGenerator(key
				.getPublicKey().getAlgorithm(), PGPUtil.SHA1, "BC");

		sGen.initSign(PGPSignature.BINARY_DOCUMENT, priK);

		Iterator it = key.getPublicKey().getUserIDs();
		if (it.hasNext()) {
			PGPSignatureSubpacketGenerator spGen = new PGPSignatureSubpacketGenerator();
			spGen.setSignerUserID(false, (String) it.next());
			sGen.setHashedSubpackets(spGen.generate());
		}

		BCPGOutputStream bOut = new BCPGOutputStream(out);

		int rSize = 0;
		byte[] buf = new byte[1024];

		while ((rSize = in.read(buf)) >= 0)
			sGen.update(buf, 0, rSize);

		PGPSignature sig = sGen.generate();
		sig.encode(bOut);

	}

	public static boolean verify(InputStream dataIn, InputStream in,
			InputStream keyIs) throws Exception {
		in = PGPUtil.getDecoderStream(in);
		// dataIn = PGPUtil.getDecoderStream(dataIn);
		PGPObjectFactory pgpFact = new PGPObjectFactory(in);
		PGPSignatureList p3 = null;

		Object o;

		try {
			o = pgpFact.nextObject();
			if (o == null)
				throw new Exception();
		} catch (Exception ex) {
			throw new Exception("Invalid input data");
		}

		if (o instanceof PGPCompressedData) {
			PGPCompressedData c1 = (PGPCompressedData) o;

			pgpFact = new PGPObjectFactory(c1.getDataStream());

			p3 = (PGPSignatureList) pgpFact.nextObject();
		} else {
			p3 = (PGPSignatureList) o;
		}

		int ch;

		PGPSignature sig = p3.get(0);
		PGPPublicKeyRingCollection pubring = parsePGPPublicKeyRingCollection(keyIs);
		PGPPublicKey key = pubring.getPublicKey(sig.getKeyID());
		if (key == null)
			throw new Exception("Cannot find key 0x"
					+ Integer.toHexString((int) sig.getKeyID()).toUpperCase()
					+ " in the pubring");

		sig.initVerify(key, "BC");

		while ((ch = dataIn.read()) >= 0)
			sig.update((byte) ch);

		if (sig.verify())
			return true;
		else
			return false;
	}

	public static void main(String[] args) throws Exception {

		SecurityUtils.generatorKeyPair("zhouyanming", "linuxfans",
				new FileOutputStream("pub.key"),
				new FileOutputStream("pri.key"));

		SecurityUtils.encrypt(new FileInputStream("build.xml"),
				new FileOutputStream("build.xml.crpt"), new FileInputStream(
						"pub.key"));

		SecurityUtils.decrypt(new FileInputStream("build.xml.crpt"),
				new FileOutputStream("build2.xml"), new FileInputStream(
						"pri.key"), "linuxfans".toCharArray());

		SecurityUtils.sign(new FileInputStream("build.xml"),
				new FileOutputStream("build.xml.sign"), new FileInputStream(
						"pri.key"), "linuxfans".toCharArray());

		boolean is = SecurityUtils.verify(new FileInputStream("build.xml"),
				new FileInputStream("build.xml.sign"), new FileInputStream(
						"pub.key"));

		System.out.println(is);

	}
}
