/******************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 1999-2006 ComPiere, Inc. All Rights Reserved.                *
 * This program is free software; you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program; if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * ComPiere, Inc., 2620 Augustine Dr. #245, Santa Clara, CA 95054, USA        *
 * or via info@compiere.org or http://www.compiere.org/license.html           *
 *****************************************************************************/
package org.compiere.print.layout;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.Properties;

import org.compiere.print.MPrintFont;
import org.compiere.print.MPrintFormatItem;
import org.krysalis.barcode4j.BarcodeDimension;
import org.krysalis.barcode4j.ChecksumMode;
import org.krysalis.barcode4j.HumanReadablePlacement;
import org.krysalis.barcode4j.impl.AbstractBarcodeBean;
import org.krysalis.barcode4j.impl.code39.Code39Bean;
import org.krysalis.barcode4j.impl.qr.QRCodeBean;
import org.krysalis.barcode4j.impl.upcean.UPCABean;
import org.krysalis.barcode4j.output.bitmap.BitmapCanvasProvider;

import net.sourceforge.barbecue.Barcode;
import net.sourceforge.barbecue.BarcodeFactory;
import net.sourceforge.barbecue.BarcodeImageHandler;
import net.sourceforge.barbecue.linear.ean.UCCEAN128Barcode;

/**
 * 	Barcode Print Element
 *	
 *  @author Jorg Janke
 *  @version $Id: BarcodeElement.java,v 1.2 2006/07/30 00:53:02 jjanke Exp $
 * 
 * @author Teo Sarca, SC ARHIPAC SERVICE SRL
 * 			<li>FR [ 1803359 ] Migrate to barbecue 1.1
 */
public class BarcodeElement extends PrintElement
{
	/**
	 * generated serial id
	 */
	private static final long serialVersionUID = -6825913765885213717L;

	/**
	 * 	Barcode Element Constructor
	 *	@param code barcode data string
	 *	@param item format item
	 */
	public BarcodeElement (String code, MPrintFormatItem item)
	{
		super ();
		if (code == null || code.length() == 0
			|| item == null
			|| item.getBarcodeType() == null || item.getBarcodeType().length() == 0)
			m_valid = false;
		
		createBarcode(code, item);
		if (m_barcode == null && m_barcodeBean == null)
			m_valid = false;
		m_allowOverflow = item.isHeightOneLine(); // teo_sarca, [ 1673590 ]
	}	//	BarcodeElement

	/**	Valid					*/
	private boolean 	m_valid = true;
	/**	 Barcode				*/
	private Barcode		m_barcode = null;
	/**  Allow this field to overflow over next fields */// teo_sarca, [ 1673590 ]
	private boolean m_allowOverflow = true;
	private float m_scaleFactor = 1;

	private AbstractBarcodeBean m_barcodeBean = null;
	private String m_code;
	
	/**
	 * 	Create Barcode
	 *	@param code barcode data string
	 *	@param item printformat item
	 */
	private void createBarcode(String code, MPrintFormatItem item)
	{
		String type = item.getBarcodeType();
		try
		{
			if (type.equals(MPrintFormatItem.BARCODETYPE_Codabar2Of7Linear))
				m_barcode = BarcodeFactory.create2of7(code);
			else if (type.equals(MPrintFormatItem.BARCODETYPE_CodabarMonarchLinear))
				m_barcode = BarcodeFactory.createMonarch(code);
			else if (type.equals(MPrintFormatItem.BARCODETYPE_CodabarNW_7Linear))
				m_barcode = BarcodeFactory.createNW7(code);
			else if (type.equals(MPrintFormatItem.BARCODETYPE_CodabarUSD_4Linear))
				m_barcode = BarcodeFactory.createUSD4(code);
			else if (type.equals(MPrintFormatItem.BARCODETYPE_Code128ACharacterSet))
				m_barcode = BarcodeFactory.createCode128A(code);
			else if (type.equals(MPrintFormatItem.BARCODETYPE_Code128BCharacterSet))
				m_barcode = BarcodeFactory.createCode128B(code);
			else if (type.equals(MPrintFormatItem.BARCODETYPE_Code128CCharacterSet))
				m_barcode = BarcodeFactory.createCode128C(code);
			else if (type.equals(MPrintFormatItem.BARCODETYPE_Code128DynamicallySwitching))
				m_barcode = BarcodeFactory.createCode128(code);
			else if (type.equals(MPrintFormatItem.BARCODETYPE_Code393Of9LinearWithChecksum))
				m_barcode = BarcodeFactory.create3of9(code, true);
			else if (type.equals(MPrintFormatItem.BARCODETYPE_Code393Of9LinearWOChecksum))
				m_barcode = BarcodeFactory.create3of9(code, false);
			else if (type.equals(MPrintFormatItem.BARCODETYPE_Code39LinearWithChecksum))
				m_barcode = BarcodeFactory.createCode39(code, true);
			else if (type.equals(MPrintFormatItem.BARCODETYPE_Code39LinearWOChecksum))
				m_barcode = BarcodeFactory.createCode39(code, false);
			else if (type.equals(MPrintFormatItem.BARCODETYPE_Code39USD3WithChecksum))
				m_barcode = BarcodeFactory.createUSD3(code, true);
			else if (type.equals(MPrintFormatItem.BARCODETYPE_Code39USD3WOChecksum))
				m_barcode = BarcodeFactory.createUSD3(code, false);
			else if (type.equals(MPrintFormatItem.BARCODETYPE_CodeabarLinear))
				m_barcode = BarcodeFactory.createCodabar(code);
			
			//	http://www.idautomation.com/code128faq.html
			else if (type.equals(MPrintFormatItem.BARCODETYPE_EAN128))
				m_barcode = BarcodeFactory.createEAN128(code);
			else if (type.equals(MPrintFormatItem.BARCODETYPE_GlobalTradeItemNoGTINUCCEAN128))
				m_barcode = BarcodeFactory.createGlobalTradeItemNumber(code);
			else if (type.equals(MPrintFormatItem.BARCODETYPE_PDF417TwoDimensional))
				m_barcode = BarcodeFactory.createPDF417(code);
			else if (type.equals(MPrintFormatItem.BARCODETYPE_SCC_14ShippingCodeUCCEAN128))
				m_barcode = BarcodeFactory.createSCC14ShippingCode(code);
			else if (type.equals(MPrintFormatItem.BARCODETYPE_ShipmentIDNumberUCCEAN128))
				m_barcode = BarcodeFactory.createShipmentIdentificationNumber(code);
			else if (type.equals(MPrintFormatItem.BARCODETYPE_SSCC_18NumberUCCEAN128))
				m_barcode = BarcodeFactory.createSSCC18(code);
			else if (type.equals(MPrintFormatItem.BARCODETYPE_UCC128))
				m_barcode = BarcodeFactory.createUCC128(UCCEAN128Barcode.EAN128_AI, code);
			else if (type.equals(MPrintFormatItem.BARCODETYPE_EAN13)) //@Trifon
				m_barcode = BarcodeFactory.createEAN13(code); //@Trifon
			else if (type.equals(MPrintFormatItem.BARCODETYPE_UPC_A)) {				
				m_barcodeBean = new UPCABean();
				((UPCABean)m_barcodeBean).setChecksumMode(ChecksumMode.CP_AUTO);
				if (item.isPrintBarcodeText())
					m_barcodeBean.setMsgPosition(HumanReadablePlacement.HRP_BOTTOM);
				else
					m_barcodeBean.setMsgPosition(HumanReadablePlacement.HRP_NONE);
				m_code = code;				
			}
			else if (type.equals(MPrintFormatItem.BARCODETYPE_Code39WithChecksum))
			{
				m_barcodeBean = new Code39Bean();
				((Code39Bean)m_barcodeBean).setChecksumMode(ChecksumMode.CP_AUTO);
				if (item.isPrintBarcodeText())
					m_barcodeBean.setMsgPosition(HumanReadablePlacement.HRP_BOTTOM);
				else
					m_barcodeBean.setMsgPosition(HumanReadablePlacement.HRP_NONE);
				m_code = code;
			}
			else if (type.equals(MPrintFormatItem.BARCODETYPE_Code39WOChecksum))
			{
				m_barcodeBean = new Code39Bean();
				((Code39Bean)m_barcodeBean).setChecksumMode(ChecksumMode.CP_IGNORE);
				if (item.isPrintBarcodeText())
					m_barcodeBean.setMsgPosition(HumanReadablePlacement.HRP_BOTTOM);
				else
					m_barcodeBean.setMsgPosition(HumanReadablePlacement.HRP_NONE);
				m_code = code;
			}
			//	http://www.usps.com/cpim/ftp/pubs/pub97/97apxs_006.html#_Toc481397331
			else if (type.equals(MPrintFormatItem.BARCODETYPE_USPostalServiceUCCEAN128))
			{
				m_barcode = BarcodeFactory.createUSPS(code);
			}
			else if (type.equals(MPrintFormatItem.BARCODETYPE_QRCode))
			{
				m_barcodeBean = new QRCodeBean();
				if (item.isPrintBarcodeText())
					m_barcodeBean.setMsgPosition(HumanReadablePlacement.HRP_BOTTOM);
				else
					m_barcodeBean.setMsgPosition(HumanReadablePlacement.HRP_NONE);
				m_code = code;
			}
			else 
				log.warning("Invalid Type" + type);
		}
		catch (Exception e)
		{
			log.warning(code + " - " + e.toString());
			m_valid = false;
		}		
		
		if (m_valid && m_barcode != null)
		{
			if (item.getAD_PrintFont_ID() != 0)
			{
				MPrintFont mFont = MPrintFont.get(item.getAD_PrintFont_ID());
				if (mFont != null)
					m_barcode.setFont(mFont.getFont());
			}
			m_barcode.setDrawingText(item.isPrintBarcodeText());
		}
	}	//	createBarcode
	
	/**
	 * 	Is Barcode Valid
	 *	@return true if valid
	 */
	public boolean isValid()
	{
		return m_valid;
	}	//	isValid
	
	/**
	 * 	Layout and Calculate Size.<br/>
	 * 	Set p_width and p_height.
	 * 	@return true if calculated
	 */
	protected boolean calculateSize ()
	{
		p_width = 0;
		p_height = 0;
		if (m_barcode == null && m_barcodeBean == null)
			return true;

		if (m_barcode != null)
		{			
			p_width = m_barcode.getWidth();
			p_height = m_barcode.getHeight();
		
			//convert from pixel to point/inch
			if (p_width > 0)
				p_width = p_width * 3f / 4f;
			if (p_height > 0)
				p_height = p_height * 3f / 4f;
		}
		else
		{			
			BarcodeDimension t = m_barcodeBean.calcDimensions(m_code);
			//convert from mm to point/inch
			p_width = (float) (t.getWidthPlusQuiet() / 25.4f * 72f);
			p_height = (float) (t.getHeight() / 25.4f * 72f);
			
			// * 6 for resolution of 432 dpi ( 72 * 6 )
			p_width *= 6f;
			p_height *= 6f;
		}

		if (p_width * p_height == 0)
			return true;	//	don't bother scaling and prevent div by 0

		m_scaleFactor = 1f;
		if (p_maxWidth != 0 && p_width > p_maxWidth)
			m_scaleFactor = p_maxWidth / p_width;
		if (p_maxHeight != 0 && p_height > p_maxHeight && p_maxHeight/p_height < m_scaleFactor)
			m_scaleFactor = p_maxHeight / p_height;
		else if (p_maxHeight != 0 && (m_scaleFactor * p_height) > p_maxHeight)
			m_scaleFactor = p_maxHeight / p_height;

		p_width = (float) m_scaleFactor * p_width;
		p_height = (float) m_scaleFactor * p_height;

		return true;
	}	//	calculateSize

	/**
	 * Get scale factor
	 * @return scale factor
	 */
	public float getScaleFactor() {
		if (!p_sizeCalculated)
			p_sizeCalculated = calculateSize();
		return m_scaleFactor;
	}

	/**
	 * Is barcode allow to overflows over next fields
	 * @return can this element overflow over the next fields
	 */
	public boolean isAllowOverflow() { // 
		return m_allowOverflow;
	}
	
	/**
	 * 	Paint Element
	 *	@param g2D graphics
	 *	@param pageNo page no
	 *	@param pageStart page start
	 *	@param ctx context
	 *	@param isView view
	 */
	@Override
	public void paint (Graphics2D g2D, int pageNo, Point2D pageStart,
		Properties ctx, boolean isView)
	{
		if (!m_valid || (m_barcode == null && m_barcodeBean == null ))
			return;
		
		//	Position
		Point2D.Double location = getAbsoluteLocation(pageStart);
		int x = (int)location.x;
		if (MPrintFormatItem.FIELDALIGNMENTTYPE_TrailingRight.equals(p_FieldAlignmentType))
			x += p_maxWidth - p_width;
		else if (MPrintFormatItem.FIELDALIGNMENTTYPE_Center.equals(p_FieldAlignmentType))
			x += (p_maxWidth - p_width) / 2;
		int y = (int)location.y;

		paint(g2D, x, y); 
	}	//	paint

	/**
	 * Paint element
	 * @param g2D
	 * @param x
	 * @param y
	 */
	public void paint(Graphics2D g2D, int x, int y) {
		try {
			
			BufferedImage image =  null;
			
			if (m_barcode != null)
			{
				// draw barcode to buffer
				image = BarcodeImageHandler.getImage(m_barcode);
				
			}
			else
			{
				//use resolution of 432 dpi (72 * 6) for better output
				BitmapCanvasProvider provider = new BitmapCanvasProvider(72*6, BufferedImage.TYPE_INT_ARGB, true, 0);
				m_barcodeBean.generateBarcode(provider, m_code);
				provider.finish();
				image = provider.getBufferedImage();
			}
			
			// scale barcode and paint
			AffineTransform transform = new AffineTransform();
			transform.translate(x,y);
			float scaleFactor = m_scaleFactor * 3f / 4f;
			transform.scale(scaleFactor, scaleFactor);
			g2D.drawImage(image, transform, this);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 	String Representation
	 *	@return info
	 */
	@Override
	public String toString ()
	{
		if (m_barcode == null)
			return super.toString();
		return super.toString() + " " + m_barcode.getData();
	}	//	toString
	
}	//	BarcodeElement
