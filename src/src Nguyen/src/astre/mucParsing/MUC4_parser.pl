use warnings;
use strict;

my $PATH = '/vol/zola/users/nguyen/ASTRE/Dataset/muc34/TASK/CORPORA/dev/';
my $OUT_FILE = '> /vol/zola/users/nguyen/ASTRE/Dataset/muc-dataset(cleaned)';

my $count = 1;
opendir DIR, $PATH or die;
my @paragraph = ();
my @document = ();
my $docId;
my %data = ();
while (my $f = readdir DIR) {
	if ($f =~ /dev-muc3-/) {
		open F, $PATH.$f or die;
		while (my $line = <F>) {
			chop $line;
			# if a text segment
			if ($line !~ /DEV-MUC3-/ and $line ne '') {
				push @paragraph, $line;
			# if a document id
			} elsif ($line =~ /(DEV-MUC3-0*\d+)/ or eof) {
				if (@document) {
					$data{$docId} = join ' ', @document;
				} elsif (defined $docId) {
					print $docId;
				}
				$docId = $1;
				@document = ();
			# if an empty line (new paragraph)
			} elsif ($line eq '' and @paragraph) {
				# join text segments into a paragraph
				my $text = join ' ', @paragraph;
				# remove [comments]
				$text =~ s/\[[^\]]+\]//g;
				# remove (comments)
				$text =~ s/\([^\)]+\)//g;
				# left trim
				$text =~ s/^\s+//;
				# right trim
				$text =~ s/\s+$//;
				# replace multiple spaces into a single one				
				$text =~ s/\s+/ /g;				
				# add paragraph to document
				if ($text ne '') {
					push @document, $text;
				}
				# re-initialize paragraph		
				@paragraph = (); 
			}
		}
		close F or die;
	}
}
closedir DIR or die;

open OUT, $OUT_FILE or die;
foreach my $id (sort keys %data) {
	print OUT $id, "\t", $data{$id}, "\n";
}
close OUT or die;

