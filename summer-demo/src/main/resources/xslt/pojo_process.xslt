<?xml version="1.0" encoding="iso-8859-1"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
  <xsl:output
    method="html"
    indent="yes"
    media-type="text/html"
    encoding="iso-8859-1" />
  <xsl:param
    name="myparam" />
  <xsl:variable name="labels" select="document('../xml/Labels.xml')" />
  <xsl:template match="/">
    <html>
      <head>
        <META
          HTTP-EQUIV="Expires"
          CONTENT="now" />
        <META
          HTTP-EQUIV="Pragma"
          CONTENT="no-cache" />
        <META
          HTTP-EQUIV="Cache-Control"
          CONTENT="private" />
        <title>
          TEST
        </title>
      </head>
      <body>
        <h1>TEST</h1>
        Test label: <xsl:value-of select="$labels/LABELS/@Test"/>
        <br/>
        Parameter: <xsl:value-of select="$myparam"/>
      </body>
    </html>
  </xsl:template>
</xsl:stylesheet>