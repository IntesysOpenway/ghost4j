/**
 * Copyright (C) 2014 - 2015 Intesys OpenWay.
 */
package org.ghost4j.converter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.ghost4j.Ghostscript;
import org.ghost4j.GhostscriptException;
import org.ghost4j.document.Document;
import org.ghost4j.document.DocumentException;
import org.ghost4j.document.PDFDocument;
import org.ghost4j.util.DiskStore;

/**
 * @author Luca Rubin
 * 
 */
public class PDFAConverter extends PDFConverter {

	/**
	 * 
	 */
	public PDFAConverter() {

		// set supported classes
		supportedDocumentClasses = new Class[1];
		supportedDocumentClasses[0] = PDFDocument.class;
	}

	/**
	 * Main method used to start the converter in standalone 'slave mode'.
	 * 
	 * @param args
	 * @throws ConverterException
	 */
	public static void main(String args[]) throws ConverterException {

		startRemoteConverter(new PDFAConverter());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ghost4j.converter.PDFConverter#run(org.ghost4j.document.Document,
	 * java.io.OutputStream)
	 */
	@Override
	public void run(Document document, OutputStream outputStream)
			throws IOException, ConverterException, DocumentException {

		// if no output = nothing to do
		if (outputStream == null) {
			return;
		}

		// assert document is supported
		this.assertDocumentSupported(document);

		// get Ghostscript instance
		Ghostscript gs = Ghostscript.getInstance();

		// generate a unique diskstore key
		DiskStore diskStore = DiskStore.getInstance();
		String diskStoreKey = diskStore.generateUniqueKey();
		// generate a unique diskstore key for input file
		String inputDiskStoreKey = diskStore.generateUniqueKey();
		// write document to input file
		document.write(diskStore.addFile(inputDiskStoreKey));

		// prepare Ghostscript interpreter parameters
		// refer to Ghostscript documentation for parameter usage
		List<String> lstArgs = new ArrayList<String>();

		// Parameters order is critical
		lstArgs.add("-q");
		lstArgs.add("-f");

		// pdf settings
		switch (getPDFSettings()) {
		case OPTION_PDFSETTINGS_EBOOK:
			lstArgs.add("-dPDFSETTINGS=/ebook");
			break;
		case OPTION_PDFSETTINGS_SCREEN:
			lstArgs.add("-dPDFSETTINGS=/screen");
			break;
		case OPTION_PDFSETTINGS_PRINTER:
			lstArgs.add("-dPDFSETTINGS=/printer");
			break;
		case OPTION_PDFSETTINGS_PREPRESS:
			lstArgs.add("-dPDFSETTINGS=/prepress");
			break;
		default:
			lstArgs.add("-dPDFSETTINGS=/default");
		}

		lstArgs.add("-dNOPAUSE");
		lstArgs.add("-dBATCH");

		// PDFA
		lstArgs.add("-dPDFA");

		// NOOUTERSAVE
		lstArgs.add("-dNOOUTERSAVE");

		lstArgs.add("-dSAFER");
		lstArgs.add("-sDEVICE=pdfwrite");

		// processcolormodel
		switch (getProcessColorModel()) {
		case OPTION_PROCESSCOLORMODEL_CMYK:
			lstArgs.add("-dProcessColorModel=/DeviceCMYK");
			break;
		case OPTION_PROCESSCOLORMODEL_GRAY:
			lstArgs.add("-dProcessColorModel=/DeviceGray");
			break;
		default:
			lstArgs.add("-sProcessColorModel=DeviceRGB");
		}

		// -dUseCIEColor
		lstArgs.add("-dUseCIEColor");

		// -dPDFACompatibilityPolicy=1
		lstArgs.add("-dPDFACompatibilityPolicy=1");

		// autorotatepages
		switch (getAutoRotatePages()) {
		case OPTION_AUTOROTATEPAGES_NONE:
			lstArgs.add("-dAutoRotatePages=/None");
			break;
		case OPTION_AUTOROTATEPAGES_ALL:
			lstArgs.add("-dAutoRotatePages=/All");
			break;
		case OPTION_AUTOROTATEPAGES_PAGEBYPAGE:
			lstArgs.add("-dAutoRotatePages=/PageByPage");
			break;
		default:
			// nothing
			break;
		}

		// compatibilitylevel
		lstArgs.add("-dCompatibilityLevel=" + getCompatibilityLevel());

		// output to file, as stdout redirect does not work properly
		lstArgs.add("-sOutputFile="
				+ diskStore.addFile(diskStoreKey).getAbsolutePath());
		lstArgs.add(diskStore.getFile(inputDiskStoreKey).getAbsolutePath());

		try {

			// execute and exit interpreter
			synchronized (gs) {
				gs.initialize(lstArgs.toArray(new String[lstArgs.size()]));
				gs.exit();
			}
			// write obtained file to output stream
			File outputFile = diskStore.getFile(diskStoreKey);
			if (outputFile == null) {
				throw new ConverterException("Cannot retrieve file with key "
						+ diskStoreKey + " from disk store");
			}

			FileInputStream fis = new FileInputStream(outputFile);
			byte[] content = new byte[(int) outputFile.length()];
			fis.read(content);
			fis.close();

			outputStream.write(content);

		} catch (GhostscriptException e) {

			throw new ConverterException(e);

		} finally {
			// delete Ghostscript instance
			try {
				Ghostscript.deleteInstance();
			} catch (GhostscriptException e) {
				throw new ConverterException(e);
			}

			// remove temporary file
			diskStore.removeFile(diskStoreKey);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ghost4j.converter.PDFConverter#isPDFX()
	 */
	@Override
	public boolean isPDFX() {
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ghost4j.converter.PDFConverter#setPDFX(boolean)
	 */
	@Override
	public void setPDFX(boolean PDFX) {
		// throw new UnsupportedOperationException("Metodo non supportato");
	}

}
