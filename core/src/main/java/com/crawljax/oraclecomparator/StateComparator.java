package com.crawljax.oraclecomparator;

import javax.inject.Inject;

import net.jcip.annotations.ThreadSafe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.crawljax.browser.EmbeddedBrowser;
import com.crawljax.condition.Condition;
import com.crawljax.core.configuration.CrawljaxConfiguration;
import com.google.common.collect.ImmutableList;

/**
 * Defines an Oracle Comparator which used multiple Oracles to decide whether two states are
 * equivalent.
 * 
 * @author dannyroest@gmail.com (Danny Roest)
 */
@ThreadSafe
public class StateComparator {

	private static final Logger LOGGER = LoggerFactory.getLogger(StateComparator.class.getName());

	/**
	 * This is an shared public static, as it is final and a primary type no harm can be done. Only
	 * accessed in {@link AbstractComparator#compare()}.
	 */
	public static final boolean COMPARE_IGNORE_CASE = true;

	private final ImmutableList<OracleComparator> oracleComparator;

	private final ThreadLocal<String> strippedOriginalDom = new ThreadLocal<String>();
	private final ThreadLocal<String> strippedNewDom = new ThreadLocal<String>();

	/**
	 * @param comparatorsWithPreconditions
	 *            comparators with one or more preconditions
	 */
	@Inject
	public StateComparator(CrawljaxConfiguration config) {
		oracleComparator = config.getCrawlRules().getOracleComparators();
	}

	/**
	 * @param originalDom
	 *            the original dom
	 * @param newDom
	 *            the current DOM in the browser
	 * @param browser
	 *            the current browser instance
	 * @return true iff originalDom and newDom are equivalent. Determining equivalence is done with
	 *         oracles and pre-conditions.
	 */
	public boolean compare(String originalDom, String newDom, EmbeddedBrowser browser) {
		for (OracleComparator oraclePreCondition : oracleComparator) {

			boolean allPreConditionsSucceed = true;
			// Loop over All the Preconditions of this oracle
			for (Condition preCondition : oraclePreCondition.getPreConditions()) {
				LOGGER.debug("Check precondition: " + preCondition.toString());
				if (!preCondition.check(browser)) {
					allPreConditionsSucceed = false;
					break;
				}
			}

			// use oracle if preconditions succeeds
			if (allPreConditionsSucceed) {

				Comparator oracle = oraclePreCondition.getOracle();
				LOGGER.debug("Using " + oracle.getClass().getSimpleName() + ": "
				        + oraclePreCondition.getId());

				boolean equivalent = false;
				// Synchronise on a single oracle setting the doms at first and later retrieve them
				// after synchronised processing.
				// TODO Stefan the ultimate Goal is to remove this synchronisation
				synchronized (oracle) {
					oracle.setOriginalDom(originalDom);
					oracle.setNewDom(newDom);

					equivalent = oracle.isEquivalent();

					originalDom = oracle.getOriginalDom();
					newDom = oracle.getNewDom();
				}

				if (equivalent) {
					// All preconditions succeeded & the oracle is Equivalent
					this.strippedOriginalDom.set(originalDom);
					this.strippedNewDom.set(newDom);
					return true;
				}
			}
		}
		/* Update the dom values to the last version */
		this.strippedOriginalDom.set(originalDom);
		this.strippedNewDom.set(newDom);
		return false;
	}

	/**
	 * @param browser
	 *            the current browser instance
	 * @return the stripped fom by the oracle comparators
	 */
	public String getStrippedDom(EmbeddedBrowser browser) {
		compare("", browser.getDom(), browser);
		return getStrippedNewDom();
	}

	/**
	 * @return the strippedOriginalDom
	 */
	public String getStrippedOriginalDom() {
		return strippedOriginalDom.get();
	}

	/**
	 * @return the strippedNewDom
	 */
	public String getStrippedNewDom() {
		return strippedNewDom.get();
	}

}
