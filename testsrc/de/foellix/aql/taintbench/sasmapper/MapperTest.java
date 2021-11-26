package de.foellix.aql.taintbench.sasmapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import de.foellix.aql.datastructure.Answer;
import de.foellix.aql.datastructure.handler.AnswerHandler;
import de.foellix.aql.helper.FileHelper;

public class MapperTest {
	private static final File ANSWER_DIR = new File("data/answer");

	@BeforeAll
	public static void beforeAll() {
		cleanUp();
	}

	@AfterEach
	public void after() {
		System.out.println("\n\n");
	}

	@AfterAll
	public static void afterAll() {
		cleanUp();
	}

	private void test(String args) {
		boolean noException = true;
		final Mapper mapper = new Mapper();
		boolean success = false;
		try {
			success = mapper.map(args.split(" "));
		} catch (final Exception e) {
			noException = false;
			e.printStackTrace();
		}

		assertTrue(noException);
		assertTrue(success);
	}

	private static void cleanUp() {
		FileHelper.deleteDir(ANSWER_DIR);
		ANSWER_DIR.mkdir();
	}

	// Tests
	@Test
	public void test01() {
		test("data/apk/cajino_baidu.apk");

		final Answer aqlAnswer = AnswerHandler.parseXML(new File(ANSWER_DIR, "cajino_baidu.xml"));
		assertEquals(15, aqlAnswer.getSources().getSource().size());
		assertEquals(15, aqlAnswer.getSinks().getSink().size());

		cleanUp();
	}

	@Test
	public void test02() {
		test("data/apk/cajino_baidu.apk -id 1");

		final Answer aqlAnswer = AnswerHandler.parseXML(new File(ANSWER_DIR, "cajino_baidu.xml"));
		assertEquals(1, aqlAnswer.getSources().getSource().size());
		assertEquals(1, aqlAnswer.getSinks().getSink().size());

		cleanUp();
	}

	@Test
	public void test03() {
		test("-o data/answer/aql data/apk/cajino_baidu.apk");
	}

	@Test
	public void test04() {
		test("-c fd data/answer/aql/cajino_baidu.xml");
	}

	@Test
	public void test05() {
		test("-c ad data/answer/aql/cajino_baidu.xml");
	}

	@Test
	public void test06() {
		test("-o data/answer/aql -c fd data/answer/aql/cajino_baidu.xml");
	}

	@Test
	public void test07() {
		test("-tb data/json -o data/answer/aql -c fd data/answer/aql/cajino_baidu.xml");
	}

	@Test
	public void test08() {
		test("-o data/answer/aql data/apk/fakebank_android_samp.apk");
	}

	@Test
	public void test09() {
		test("-tb data/json -o data/answer/aql -c fd data/answer/aql/fakebank_android_samp.xml");
	}
}
