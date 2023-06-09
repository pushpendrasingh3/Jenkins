//
//  Author: Hari Sekhon
//  Date: 2023-06-08 23:36:19 +0100 (Thu, 08 Jun 2023)
//
//  vim:ts=2:sts=2:sw=2:et
//
//  https://github.com/HariSekhon/Jenkins
//
//  License: see accompanying Hari Sekhon LICENSE file
//
//  If you're using my code you're welcome to connect with me on LinkedIn and optionally send me feedback to help steer this or other code I publish
//
//  https://www.linkedin.com/in/HariSekhon
//

// ========================================================================== //
//                        J e n k i n s   J o b   R e p o
// ========================================================================== //

// Returns the SCM repo url for a given Jenkins job from its XML config as returned by jenkinsJobConfigXml(jobName)

def call(jobXml) {

  // https://groovy-lang.org/processing-xml.html

  //def xmlroot = new XmlSlurper().parseText(jobXml)
  // gets java.lang.NullPointerException
  //assert xmlroot instanceof groovy.xml.slurpersupport.GPathResult

  def xmlroot = new XmlParser().parseText(jobXml)

  assert xmlroot instanceof groovy.util.Node

  repo = xmlroot.'**'.find { it.name() == 'url' }.value()[0].trim()

  return repo
}
