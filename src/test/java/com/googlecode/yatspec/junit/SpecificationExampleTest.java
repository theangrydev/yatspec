package com.googlecode.yatspec.junit;

import com.googlecode.yatspec.rendering.NotesRenderer;
import com.googlecode.yatspec.rendering.html.HtmlResultRenderer;
import com.googlecode.yatspec.rendering.html.HyperlinkRenderer;
import com.googlecode.yatspec.rendering.html.index.HtmlIndexRenderer;
import com.googlecode.yatspec.rendering.html.tagindex.HtmlTagIndexRenderer;
import com.googlecode.yatspec.state.givenwhenthen.ActionUnderTest;
import com.googlecode.yatspec.state.givenwhenthen.GivensBuilder;
import com.googlecode.yatspec.state.givenwhenthen.StateExtractor;
import com.googlecode.yatspec.state.givenwhenthen.TestState;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collection;
import java.util.List;

import static com.googlecode.yatspec.plugin.jdom.StateExtractors.getValue;
import static java.lang.Double.valueOf;
import static java.lang.Math.sqrt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

@RunWith(SpecRunner.class)
@Notes("This is a note on the whole class\n" +
        "It will preserve space")
public class SpecificationExampleTest extends TestState implements WithCustomResultListeners {
    private static final String RADICAND = "Radicand";
    private static final String RESULT = "Result";

    @Test
    @Notes("#tag-one")
    public void reallySimpleExample() {
        assertThat(sqrt(9), is(3.0));
    }

    @Test
    @Table({@Row({"9", "3.0"}),
            @Row({"16", "4.0"})})
    @Notes("#tag-one\n" +
            "#tag-two\n" +
            "This example combines table / row tests with specification and given when then")
    public void takeTheSquareRoot(String radicand, String result) throws Exception {
        given(theRadicand(radicand));
        when(weTakeTheSquareRoot());
        then(theResult(), is(valueOf(result)));
    }

    @Test
    @LinkingNote(message = "The details of how the Linking Note works can be seen in the %s", links = {LinkingNoteRendererTest.class})
    public void testWithALinkingNote() {

    }

    @Test
    public void printEmptyTestName() {

    }

    @Test
    @Table({@Row({"someParam", "varargA", "varargB"}),
            @Row({"anotherParam"})})
    public void callMethodsWithTrailingVarargs(String firstParam, String... otherParams) {
        assertThat(firstParam, Matchers.not(Matchers.isIn(otherParams)));
    }

    private GivensBuilder theRadicand(final String number) {
        return theRadicand(Integer.valueOf(number));
    }

    private GivensBuilder theRadicand(final int number) {
        return interestingGivens -> interestingGivens.add(RADICAND, number);
    }

    private ActionUnderTest weTakeTheSquareRoot() {
        return (interestingGivens, capturedInputAndOutputs) -> {
            int number = interestingGivens.getType(RADICAND, Integer.class);
            return capturedInputAndOutputs.add(RESULT, sqrt(number));
        };
    }

    private static StateExtractor<Double> theResult() {
        return getValue(RESULT, Double.class);
    }

    public Collection<SpecResultListener> getResultListeners() {
        return List.of(
                new HtmlResultRenderer().withCustomRenderer(
                        Notes.class,
                        new HyperlinkRenderer(new NotesRenderer(), "(?:#)([^\\s]+)", "<a href='http://localhost:8080/pretent-issue-tracking/$1'>$1</a>")
                ),
                new HtmlIndexRenderer(),
                new HtmlTagIndexRenderer()
        );
    }
}
