CREATE OR REPLACE FUNCTION currencyRate
(
	p_CurFrom_ID		IN	NUMBER,
	p_CurTo_ID		    IN	NUMBER,
	p_ConvDate		    IN	DATE,
	p_ConversionType_ID	IN	NUMBER,
	p_Client_ID		    IN	NUMBER,
	p_Org_ID			IN	NUMBER
)
RETURN NUMBER
/*************************************************************************
 * The contents of this file are subject to the Compiere License.  You may
 * obtain a copy of the License at    http://www.compiere.org/license.html
 * Software is on an  "AS IS" basis,  WITHOUT WARRANTY OF ANY KIND, either
 * express or implied. See the License for details. Code: Compiere ERP+CRM
 * Copyright (C) 1999-2001 Jorg Janke, ComPiere, Inc. All Rights Reserved.
 *************************************************************************
 * $Id: C_Currency_Rate.sql,v 1.1 2006/04/21 17:51:58 jjanke Exp $
 ***
 * Title:	Return Conversion Rate
 * Description:
 *		from CurrencyFrom_ID to CurrencyTo_ID
 *		Returns NULL, if rate not found
 * Test
 *		SELECT C_Currency_Rate(116, 100, null, null) FROM DUAL;     => .647169
 *		SELECT C_Currency_Rate(116, 100) FROM DUAL;                 => .647169
 ************************************************************************/
AS
	--	Currency From variables
	cf_IsEuro			CHAR(1);
	cf_IsEMUMember		CHAR(1);
	cf_EMUEntryDate	DATE;
	cf_EMURate		NUMBER;
	--	Currency To variables
	ct_IsEuro			CHAR(1);
	ct_IsEMUMember		CHAR(1);
	ct_EMUEntryDate	DATE;
	ct_EMURate		NUMBER;
	--	Triangle
	v_CurrencyFrom		NUMBER;
	v_CurrencyTo		NUMBER;
	v_CurrencyEuro		NUMBER;
	--
	v_ConvDate		    DATE := SysDate;
	v_ConversionType_ID	NUMBER := 0;
	v_Rate			    NUMBER;
BEGIN
	--	No Conversion
	IF (p_CurFrom_ID = p_CurTo_ID) THEN
		RETURN 1;
	END IF;
	--	Default Date Parameter
	IF (p_ConvDate IS NOT NULL) THEN
		v_ConvDate := p_ConvDate;   --  SysDate
	END IF;
    --  Default Conversion Type
	IF (p_ConversionType_ID IS NULL OR p_ConversionType_ID = 0) THEN
        BEGIN
            SELECT C_ConversionType_ID 
            INTO v_ConversionType_ID
            FROM (
                SELECT C_ConversionType_ID 
                FROM C_ConversionType 
                WHERE IsActive='Y' AND IsDefault='Y'
                    AND AD_Client_ID IN (0,p_Client_ID)
                ORDER BY AD_Client_ID DESC
            )
            WHERE ROWNUM=1;
        EXCEPTION WHEN OTHERS THEN
            DBMS_OUTPUT.PUT_LINE('Conversion Type Not Found');
        END;
    ELSE
        v_ConversionType_ID := p_ConversionType_ID;
	END IF;

	--	Get Currency Info
	SELECT	MAX(TO_CHAR(IsEuro)), MAX(TO_CHAR(IsEMUMember)), MAX(EMUEntryDate), MAX(EMURate)
	  INTO	cf_IsEuro, cf_IsEMUMember, cf_EMUEntryDate, cf_EMURate
	FROM		C_Currency
	  WHERE	C_Currency_ID = p_CurFrom_ID;
	-- Not Found
	IF (cf_IsEuro IS NULL) THEN
		DBMS_OUTPUT.PUT_LINE('From Currency Not Found');
		RETURN NULL;
	END IF;
	SELECT	MAX(TO_CHAR(IsEuro)), MAX(TO_CHAR(IsEMUMember)), MAX(EMUEntryDate), MAX(EMURate)
	  INTO	ct_IsEuro, ct_IsEMUMember, ct_EMUEntryDate, ct_EMURate
	FROM		C_Currency
	  WHERE	C_Currency_ID = p_CurTo_ID;
	-- Not Found
	IF (ct_IsEuro IS NULL) THEN
		DBMS_OUTPUT.PUT_LINE('To Currency Not Found');
		RETURN NULL;
	END IF;

	--	Fixed - From Euro to EMU
	IF (cf_IsEuro = 'Y' AND ct_IsEMUMember ='Y' AND v_ConvDate >= ct_EMUEntryDate) THEN
		RETURN ct_EMURate;
	END IF;

	--	Fixed - From EMU to Euro
	IF (ct_IsEuro = 'Y' AND cf_IsEMUMember ='Y' AND v_ConvDate >= cf_EMUEntryDate) THEN
		RETURN 1 / cf_EMURate;
	END IF;

	--	Fixed - From EMU to EMU
	IF (cf_IsEMUMember = 'Y' AND cf_IsEMUMember ='Y'
			AND v_ConvDate >= cf_EMUEntryDate AND v_ConvDate >= ct_EMUEntryDate) THEN
		RETURN ct_EMURate / cf_EMURate;
	END IF;

	--	Flexible Rates
	v_CurrencyFrom := p_CurFrom_ID;
	v_CurrencyTo := p_CurTo_ID;

	-- if EMU Member involved, replace From/To Currency
	IF ((cf_isEMUMember = 'Y' AND v_ConvDate >= cf_EMUEntryDate)
	  OR (ct_isEMUMember = 'Y' AND v_ConvDate >= ct_EMUEntryDate)) THEN
		SELECT	MAX(C_Currency_ID)
		  INTO	v_CurrencyEuro
		FROM		C_Currency
		WHERE	IsEuro = 'Y';
		-- Conversion Rate not Found
		IF (v_CurrencyEuro IS NULL) THEN
			DBMS_OUTPUT.PUT_LINE('Euro Not Found');
			RETURN NULL;
		END IF;
		IF (cf_isEMUMember = 'Y' AND v_ConvDate >= cf_EMUEntryDate) THEN
			v_CurrencyFrom := v_CurrencyEuro;
		ELSE
			v_CurrencyTo := v_CurrencyEuro;
		END IF;
	END IF;

	--	Get Rate
	DECLARE
		CURSOR	CUR_Rate	IS
			SELECT	MultiplyRate
			FROM	C_Conversion_Rate
			WHERE	IsActive='Y' AND C_Currency_ID=v_CurrencyFrom AND C_Currency_ID_To=v_CurrencyTo
			  AND	C_ConversionType_ID=v_ConversionType_ID
			  AND	TRUNC(v_ConvDate) BETWEEN ValidFrom AND ValidTo
			  AND	AD_Client_ID IN (0,p_Client_ID) AND AD_Org_ID IN (0,p_Org_ID)
			ORDER BY AD_Client_ID DESC, AD_Org_ID DESC, ValidFrom DESC;
	BEGIN
		FOR c IN CUR_Rate LOOP
			v_Rate := c.MultiplyRate;
			EXIT;	--	only first
		END LOOP;
	END;
	--	Not found
	IF (v_Rate IS NULL) THEN
		DBMS_OUTPUT.PUT_LINE('Conversion Rate Not Found');
		RETURN NULL;
	END IF;

	--	Currency From was EMU
	IF (cf_isEMUMember = 'Y' AND v_ConvDate >= cf_EMUEntryDate) THEN
		RETURN v_Rate / cf_EMURate;
	END IF;

	--	Currency To was EMU
	IF (ct_isEMUMember = 'Y' AND v_ConvDate >= ct_EMUEntryDate) THEN
		RETURN v_Rate * ct_EMURate;
	END IF;

	RETURN v_Rate;

EXCEPTION WHEN OTHERS THEN
	DBMS_OUTPUT.PUT_LINE(SQLERRM);
	RETURN NULL;

END currencyRate;
/
