package io.paperdb;

import android.support.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;

import io.paperdb.testdata.Person;
import io.paperdb.testdata.TestDataGenerator;

import static junit.framework.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public class StorageTest {

    private DbStoragePlainFile storage;
    private static final File FILES_DIRECTORY = new File("storage.dir");

    @Before
    public void setUp() throws Exception {
        storage = new DbStoragePlainFile(FILES_DIRECTORY, Paper.DEFAULT_DB_NAME);
    }

    @Test
    public void smokeInsertTest() {
        Person person = TestDataGenerator.genPerson(0);
        storage.insert("testPerson", person);
        Person retrievedPerson = storage.select("testPerson");
        assertEquals(person,retrievedPerson);
    }

    @Test
    public void smokeInsertWithRelation() {
        Person person = TestDataGenerator.genPerson(1);
        Person relatedPerson = TestDataGenerator.genPerson(2);
        person.setRelative(relatedPerson);
        storage.insert("testPerson", person);
        Person retrievedPerson = storage.select("testPerson");
        assertEquals(person, retrievedPerson);
        assertEquals(relatedPerson,retrievedPerson.getRelative());
    }

    @Test
    public void smokeInsertWithSelfReference() {
        Person person = TestDataGenerator.genPerson(1);
        person.setRelative(person);
        storage.insert("testPerson", person);
        Person retrievedPerson = storage.select("testPerson");
        //assertEquals(person, retrievedPerson); //Causes stackoverflow on equels. TODO: write function to compare objects with cyclic references
        assertEquals(retrievedPerson, retrievedPerson.getRelative());
    }

    @Test
    public void smokeInsertWithCyclicReference() {
        Person person = TestDataGenerator.genPerson(1);
        Person relatedPerson = TestDataGenerator.genPerson(2);
        person.setRelative(relatedPerson);
        relatedPerson.setRelative(relatedPerson);

        storage.insert("testPerson", person);
        Person retrievedPerson = storage.select("testPerson");
        //assertEquals(person, retrievedPerson); //Causes stackoverflow on equels. TODO: write function to compare objects with cyclic references
        assertEquals(retrievedPerson.getRelative(), retrievedPerson.getRelative().getRelative());
    }

    @After
    public void clear() throws Exception {
        storage.destroy();
    }
}
